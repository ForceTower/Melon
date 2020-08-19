package com.forcetower.uefs.core.storage.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.forcetower.core.extensions.removeSeconds
import com.forcetower.core.getDynamicDataSourceFactory
import com.forcetower.uefs.core.model.unes.*
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.util.VersionUtils
import com.forcetower.uefs.feature.shared.extensions.createTimeInt
import com.forcetower.uefs.feature.shared.extensions.toLongWeekDay
import com.forcetower.uefs.feature.shared.extensions.toWeekDay
import com.forcetower.uefs.service.NotificationCreator
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dev.forcetower.breaker.Orchestra
import dev.forcetower.breaker.model.*
import dev.forcetower.breaker.result.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnowpiercerSyncRepository @Inject constructor(
    client: OkHttpClient,
    private val context: Context,
    private val database: UDatabase,
    private val service: UService,
    private val remoteConfig: FirebaseRemoteConfig,
    private val preferences: SharedPreferences
) {
    private val orchestra = Orchestra.Builder().client(client).build()

    @WorkerThread
    suspend fun performSync(executor: String) {
        val registry = createRegistry(executor)
        val access = database.accessDao().getAccessDirect()
        access ?: Timber.d("Access is null, sync will not continue")
        if (access != null) {
            FirebaseCrashlytics.getInstance().setUserId(access.username)
            execute(access, registry)
        } else {
            registry.completed = true
            registry.error = -1
            registry.success = false
            registry.message = "Credenciais de acesso inválidas"
            registry.end = System.currentTimeMillis()
            database.syncRegistryDao().insert(registry)
        }
    }

    private suspend fun execute(access: Access, registry: SyncRegistry) {
        val uid = database.syncRegistryDao().insert(registry)
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        registry.uid = uid

        try {
            findAndMatch()
        } catch (t: Throwable) { }

        database.gradesDao().markAllNotified()
        database.messageDao().setAllNotified()
        database.classMaterialDao().markAllNotified()

        orchestra.setAuthorization(Authorization(access.username, access.password))

        val login = orchestra.login()
        if (login is Outcome.Error) {
            if (login.code == 401) onAccessInvalided()
            else produceErrorMessage(login)
            // The first requisition failed... stop here
            return
        }

        val person = (login as Outcome.Success).value
        val localProfileId = defineUser(person)

        val messagesOutcome = orchestra.messages(person.id)
        (messagesOutcome as? Outcome.Success)?.let { success ->
            val page = success.value
            defineMessages(page)
        }

        val semestersOutcome = orchestra.semesters(person.id)
        val currentSemester = (semestersOutcome as? Outcome.Success)?.let { success ->
            val semesters = success.value
            defineSemesters(semesters)

            val current = semesters.maxByOrNull { it.id }
            current
        }

        // if no current semester... back off
        currentSemester?.let { semester ->
            val gradesOutcome = orchestra.grades(person.id, semester.id)
            (gradesOutcome as? Outcome.Success)?.let { success ->
                val disciplines = success.value
                val currentSemesterIns = database.semesterDao().getSemesterDirect(semester.id)!!
                defineDisciplinesData(disciplines, currentSemesterIns.uid, localProfileId)
            }
        }

        registry.completed = true
        registry.error = 0
        registry.success = true
        registry.skipped = 0
        registry.message = "Completo"
        registry.end = System.currentTimeMillis()
        database.syncRegistryDao().update(registry)

    }

    private suspend fun defineDisciplinesData(disciplines: List<DisciplineData>, semesterId: Long, localProfileId: Long) {
        database.withTransaction {
            val allocations = mutableListOf<ClassLocation>()
            disciplines.forEach {
                val resume = if (it.program.isNullOrBlank()) null else it.program
                val discipline = Discipline(name = it.name, code = it.code, credits = it.hours, resume = resume, department = it.department)
                val disciplineId = database.disciplineDao().insertOrUpdate(discipline)
                Timber.d("Discipline id inserted: $disciplineId at $semesterId")
                val bound = Class(
                    disciplineId = disciplineId,
                    semesterId = semesterId,
                    scheduleOnly = false,
                    missedClasses = it.result?.missedClasses ?: 0,
                    finalScore = it.result?.mean
                )

                val classId = database.classDao().insertNewWays(bound)
                it.classes.forEach { clazz ->
                    val group = ClassGroup(
                        classId = classId,
                        credits = clazz.hours,
                        draft = false,
                        group = clazz.groupName,
                        teacher = clazz.teacher.name
                    )
                    val groupId = database.classGroupDao().insertNewWay(group)
                    clazz.allocations.forEach { allocation ->
                        val time = allocation.time
                        if (time != null) {
                            allocations.add(ClassLocation(
                                groupId = groupId,
                                campus = allocation.space?.campus,
                                modulo = allocation.space?.modulo,
                                room = allocation.space?.location,
                                day = time.day.toWeekDay(),
                                dayInt = time.day,
                                startsAt = time.start.removeSeconds(),
                                endsAt = time.end.removeSeconds(),
                                startsAtInt = time.start.createTimeInt(),
                                endsAtInt = time.end.createTimeInt(),
                                profileId = localProfileId
                            ))
                        }
                    }
                }
                database.gradesDao().putGradesNewWay(classId, it.evaluations)
            }
            database.classLocationDao().putNewSchedule(allocations)
        }
    }

    private fun defineMessages(page: MessagesDataPage) {
        val messages = page.messages
        database.messageDao().insertIgnoring(messages.map { Message.fromMessage(it, false) })

        val newMessages = database.messageDao().getNewMessages()
        database.messageDao().setAllNotified()
        newMessages.forEach { it.notify(context) }
    }

    private suspend fun defineSemesters(semesters: List<dev.forcetower.breaker.model.Semester>) {
        database.withTransaction {
            semesters.forEach {
                val semester = Semester(sagresId = it.id, name = it.code, codename = it.description)
                database.semesterDao().insertIgnoring(semester)
            }
        }
    }

    private suspend fun defineUser(person: Person): Long {
        return database.profileDao().insert(person)
    }

    private fun onAccessInvalided() {
        database.accessDao().setAccessValidation(false)
        NotificationCreator.showInvalidAccessNotification(context)
    }

    @WorkerThread
    private fun findAndMatch() {
        val aeri = getDynamicDataSourceFactory(context, "com.forcetower.uefs.aeri.domain.AERIDataSourceFactoryProvider")
        aeri?.create()?.run {
            update()
            getNotifyMessages().forEach {
                NotificationCreator.showSimpleNotification(context, it.title, it.content)
            }
        }
    }

    private fun createRegistry(executor: String): SyncRegistry {
        val connectivity = ContextCompat.getSystemService(context, ConnectivityManager::class.java)!!

        if (VersionUtils.isMarshmallow()) {
            val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
            return if (capabilities != null) {
                val wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                val network = if (wifi) {
                    val manager = context.getSystemService(WifiManager::class.java)
                    manager?.connectionInfo?.ssid ?: "Unknown"
                } else {
                    val manager = context.getSystemService(TelephonyManager::class.java)
                    manager?.simOperatorName ?: "Operator"
                }
                Timber.d("Is on Wifi? $wifi. Network name: $network")
                SyncRegistry(
                        executor = executor, network = network,
                        networkType = if (wifi) NetworkType.WIFI.ordinal else NetworkType.CELLULAR.ordinal
                )
            } else {
                SyncRegistry(executor = executor, network = "Invalid", networkType = NetworkType.OTHER.ordinal)
            }
        } else {
            val manager = ContextCompat.getSystemService(context, WifiManager::class.java)
            val info = manager?.connectionInfo
            return if (info == null) {
                val phone = ContextCompat.getSystemService(context, TelephonyManager::class.java)
                val operatorName = phone?.simOperatorName
                SyncRegistry(executor = executor, network = operatorName ?: "invalid", networkType = NetworkType.CELLULAR.ordinal)
            } else {
                SyncRegistry(executor = executor, network = info.ssid, networkType = NetworkType.WIFI.ordinal)
            }
        }
    }

    private fun produceErrorMessage(outcome: Outcome.Error<*>) {
        Timber.e("Failed executing with status ${outcome.code} and throwable message [${outcome.error.message}]")
    }

    suspend fun asyncSync() = withContext(Dispatchers.IO) {
        performSync("Manual")
    }
}