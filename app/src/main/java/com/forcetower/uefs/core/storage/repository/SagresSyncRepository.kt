/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
import com.crashlytics.android.Crashlytics
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SCalendar
import com.forcetower.sagres.database.model.SDiscipline
import com.forcetower.sagres.database.model.SDisciplineClassLocation
import com.forcetower.sagres.database.model.SDisciplineGroup
import com.forcetower.sagres.database.model.SDisciplineMissedClass
import com.forcetower.sagres.database.model.SGrade
import com.forcetower.sagres.database.model.SPerson
import com.forcetower.sagres.database.model.SRequestedService
import com.forcetower.sagres.operation.BaseCallback
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.parsers.SagresBasicParser
import com.forcetower.sagres.parsers.SagresScheduleParser
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.R
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.model.unes.CalendarItem
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.NetworkType
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.model.unes.SagresFlags
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.model.unes.ServiceRequest
import com.forcetower.uefs.core.model.unes.SyncRegistry
import com.forcetower.uefs.core.model.unes.notify
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.util.VersionUtils
import com.forcetower.uefs.service.NotificationCreator
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.jsoup.nodes.Document
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SagresSyncRepository @Inject constructor(
    private val context: Context,
    private val database: UDatabase,
    private val executors: AppExecutors,
    private val adventureRepository: AdventureRepository,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val firebaseAuth: FirebaseAuth,
    @Named(Profile.COLLECTION)
    private val collection: CollectionReference,
    private val service: UService,
    private val remoteConfig: FirebaseRemoteConfig,
    private val preferences: SharedPreferences,
    private val scheduleRepository: ScheduleRepository
) {

    @WorkerThread
    fun performSync(executor: String) {
        val registry = createRegistry(executor)
        val access = database.accessDao().getAccessDirect()
        access ?: Timber.d("Access is null, sync will not continue")
        if (access != null) {
            Crashlytics.setUserIdentifier(access.username)
            // Only one sync may be active at a time
            synchronized(S_LOCK) { execute(access, registry, executor) }
        } else {
            registry.completed = true
            registry.error = -1
            registry.success = false
            registry.message = "Credenciais de acesso inválidas"
            registry.end = System.currentTimeMillis()
            database.syncRegistryDao().insert(registry)
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
                    manager.connectionInfo.ssid
                } else {
                    val manager = context.getSystemService(TelephonyManager::class.java)
                    manager.simOperatorName
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
                if (operatorName != null) {
                    SyncRegistry(executor = executor, network = operatorName, networkType = NetworkType.CELLULAR.ordinal)
                } else {
                    SyncRegistry(executor = executor, network = "Invalid", networkType = NetworkType.OTHER.ordinal)
                }
            } else {
                SyncRegistry(executor = executor, network = info.ssid, networkType = NetworkType.WIFI.ordinal)
            }
        }
    }

    @WorkerThread
    private fun execute(access: Access, registry: SyncRegistry, executor: String) {
        val uid = database.syncRegistryDao().insert(registry)
        registry.uid = uid

        if (!Constants.EXECUTOR_WHITELIST.contains(executor.toLowerCase())) {
            try {
                val call = service.getUpdate()
                val response = call.execute()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && !body.manager) {
                        registry.completed = true
                        registry.error = -4
                        registry.success = false
                        registry.message = "Atualização negada"
                        registry.end = System.currentTimeMillis()
                        database.syncRegistryDao().update(registry)
                        return
                    }
                }
            } catch (t: Throwable) {
                Timber.e(t)
                Timber.d("An error just happened... It will complete anyways")
            }
        }

        database.gradesDao().markAllNotified()
        database.messageDao().setAllNotified()
        val homeDoc = login(access)
        val score = SagresBasicParser.getScore(homeDoc)
        Timber.d("Login Completed. Score Parsed: $score")
        if (homeDoc == null) {
            registry.completed = true
            registry.error = -2
            registry.success = false
            registry.message = "Login falhou"
            registry.end = System.currentTimeMillis()
            database.syncRegistryDao().update(registry)
            return
        }

        defineSchedule(SagresScheduleParser.getSchedule(homeDoc))

        val person = me(score, homeDoc, access)
        if (person == null) {
            registry.completed = true
            registry.error = -3
            registry.success = false
            registry.message = "Busca de usuário falhou no Sagres"
            registry.end = System.currentTimeMillis()
            database.syncRegistryDao().update(registry)
            return
        }

        executors.others().execute {
            try {
                val reconnect = preferences.getBoolean("firebase_reconnect_update", true)
                firebaseAuthRepository.loginToFirebase(person, access, reconnect)
                preferences.edit().putBoolean("firebase_reconnect_update", false).apply()
            } catch (t: Throwable) {
                Crashlytics.logException(t)
            }
        }

        var result = 0
        if (access.username.contains("@") || person.isMocked) {
            if (!messages(null)) result += 1 shl 1
        } else {
            if (!messages(person.id)) result += 1 shl 1
            if (!semesters(person.id)) result += 1 shl 2
        }
        if (!startPage()) result += 1 shl 3
        if (!grades()) result += 1 shl 4
        if (!servicesRequest()) result += 1 shl 5

        try {
            val day = preferences.getInt("sync_daily_update", -1)
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            val user = firebaseAuth.currentUser
            if (user != null && day != today) {
                val instance = FirebaseInstanceId.getInstance().instanceId
                val instanceId = Tasks.await(instance)
                val data = mapOf("firebaseToken" to instanceId.token)
                val task = collection.document(user.uid).set(data, SetOptions.merge())
                Tasks.await(task)
                preferences.edit().putInt("sync_daily_update", today).apply()
                adventureRepository.performCheckAchievements(HashMap())
                scheduleRepository.saveSchedule(user.uid)
            }
            createNewVersionNotification()
        } catch (t: Throwable) {
            Crashlytics.logException(t)
        }

        registry.completed = true
        registry.error = result
        registry.success = result == 0
        registry.message = "Deve-se consultar as flags de erro"
        registry.end = System.currentTimeMillis()
        database.syncRegistryDao().update(registry)
    }

    private fun createNewVersionNotification() {
        val currentVersion = remoteConfig.getLong("version_current")
        val notified = preferences.getBoolean("version_ntf_key_$currentVersion", false)
        if (currentVersion > BuildConfig.VERSION_CODE && !notified) {
            val notes = remoteConfig.getString("version_notes")
            val version = remoteConfig.getString("version_name")
            val title = context.getString(R.string.new_version_ntf_title_format, version)
            NotificationCreator.showSimpleNotification(context, title, notes)
            preferences.edit().putBoolean("version_ntf_key_$currentVersion", true).apply()
        }
    }

    fun login(access: Access): Document? {
        val login = SagresNavigator.instance.login(access.username, access.password)
        when (login.status) {
            Status.SUCCESS -> {
                return login.document
            }
            Status.INVALID_LOGIN -> {
                onInvalidLogin()
            }
            else -> produceErrorMessage(login)
        }
        return null
    }

    private fun onInvalidLogin() {
        val access = database.accessDao().getAccessDirect()
        if (access != null && access.valid) {
            database.accessDao().setAccessValidation(false)
            NotificationCreator.showInvalidAccessNotification(context)
        }
    }

    private fun me(score: Double, document: Document, access: Access): SPerson? {
        val username = access.username
        if (username.contains("@")) {
            val person = continueWithHtml(document, username, score)
            return person
        } else {
            val me = SagresNavigator.instance.me()
            when (me.status) {
                Status.SUCCESS -> {
                    val person = me.person
                    if (person != null) {
                        database.profileDao().insert(person, score)
                        Timber.d("Me completed. Person name: ${person.name}")
                        return person
                    } else {
                        Timber.e("Page loaded but API returned invalid types")
                    }
                }
                Status.RESPONSE_FAILED -> {
                    val name = SagresBasicParser.getName(document) ?: username
                    return SPerson(username.hashCode().toLong(), name, name, "00000000000", username).apply { isMocked = true }
                }
                else -> produceErrorMessage(me)
            }
        }
        return null
    }

    private fun continueWithHtml(document: Document, username: String, score: Double): SPerson {
        val name = SagresBasicParser.getName(document) ?: username
        val person = SPerson(username.hashCode().toLong(), name, name, "00000000000", username).apply { isMocked = true }
        database.profileDao().insert(person, score)
        return person
    }

    @WorkerThread
    private fun messages(userId: Long?): Boolean {
        val messages = if (userId != null)
            SagresNavigator.instance.messages(userId)
        else
            SagresNavigator.instance.messagesHtml()

        return when (messages.status) {
            Status.SUCCESS -> {
                val values = messages.messages?.map { Message.fromMessage(it, false) } ?: emptyList()
                database.messageDao().insertIgnoring(values)
                messagesNotifications()
                Timber.d("Messages completed. Messages size is ${values.size}")
                true
            }
            else -> {
                produceErrorMessage(messages)
                false
            }
        }
    }

    @WorkerThread
    private fun semesters(userId: Long): Boolean {
        val semesters = SagresNavigator.instance.semesters(userId)
        return when (semesters.status) {
            Status.SUCCESS -> {
                val values = semesters.getSemesters().map { Semester.fromSagres(it) }
                database.semesterDao().insertIgnoring(values)
                Timber.d("Semesters Completed with: ${semesters.getSemesters()}")
                true
            }
            else -> {
                produceErrorMessage(semesters)
                false
            }
        }
    }

    @WorkerThread
    private fun startPage(): Boolean {
        val start = SagresNavigator.instance.startPage()
        return when (start.status) {
            Status.SUCCESS -> {
                defineCalendar(start.calendar)
                defineDisciplines(start.disciplines)
                defineDisciplineGroups(start.groups)
                defineSchedule(start.locations)
                defineDemand(start.isDemandOpen)

                Timber.d("Semesters: ${start.semesters}")
                Timber.d("Disciplines:  ${start.disciplines}")
                Timber.d("Calendar: ${start.calendar}")
                true
            }
            else -> {
                produceErrorMessage(start)
                false
            }
        }
    }

    @WorkerThread
    private fun grades(): Boolean {
        val grades = SagresNavigator.instance.getCurrentGrades()
        return when (grades.status) {
            Status.SUCCESS -> {
                defineSemesters(grades.semesters)
                defineGrades(grades.grades)
                defineFrequency(grades.frequency)

                Timber.d("Grades received: ${grades.grades}")
                Timber.d("Frequency: ${grades.frequency}")
                Timber.d("Semesters: ${grades.semesters}")

                gradesNotifications()
                frequencyNotifications()

                Timber.d("Completed!")
                true
            }
            else -> {
                produceErrorMessage(grades)
                false
            }
        }
    }

    @WorkerThread
    private fun servicesRequest(): Boolean {
        val services = SagresNavigator.instance.getRequestedServices()
        return when (services.status) {
            Status.SUCCESS -> {
                defineServices(services.services)
                Timber.d("Services Requested: ${services.services}")
                servicesNotifications()
                true
            }
            else -> {
                produceErrorMessage(services)
                false
            }
        }
    }

    private fun servicesNotifications() {
        database.serviceRequestDao().run {
            val created = getCreatedDirect()
            val updated = getStatusChangedDirect()

            created.forEach { NotificationCreator.createServiceRequestNotification(context, it, false) }
            updated.forEach { NotificationCreator.createServiceRequestNotification(context, it, true) }

            markAllNotified()
        }
    }

    private fun defineServices(services: List<SRequestedService>) {
        val list = services.map { ServiceRequest.fromSagres(it) }
        database.serviceRequestDao().insertList(list)
    }

    private fun frequencyNotifications() {
        // TODO Implement frequency notifications
    }

    private fun gradesNotifications() {
        database.gradesDao().run {
            val posted = getPostedGradesDirect()
            val create = getCreatedGradesDirect()
            val change = getChangedGradesDirect()
            val date = getDateChangedGradesDirect()

            markAllNotified()

            posted.forEach { NotificationCreator.showSagresPostedGradesNotification(it, context) }
            create.forEach { NotificationCreator.showSagresCreateGradesNotification(it, context) }
            change.forEach { NotificationCreator.showSagresChangeGradesNotification(it, context) }
            date.forEach { NotificationCreator.showSagresDateGradesNotification(it, context) }
        }
    }

    @WorkerThread
    private fun messagesNotifications() {
        val messages = database.messageDao().getNewMessages()
        database.messageDao().setAllNotified()
        messages.forEach { it.notify(context) }
    }

    @WorkerThread
    private fun defineFrequency(frequency: List<SDisciplineMissedClass>?) {
        if (frequency == null) return
        database.classAbsenceDao().putAbsences(frequency)
    }

    @WorkerThread
    private fun defineGrades(grades: List<SGrade>) {
        database.gradesDao().putGrades(grades)
    }

    @WorkerThread
    private fun defineSemesters(semesters: List<Pair<Long, String>>) {
        semesters.forEach {
            val semester = Semester(sagresId = it.first, name = it.second, codename = it.second)
            database.semesterDao().insertIgnoring(semester)
        }
    }

    @WorkerThread
    private fun defineSchedule(locations: List<SDisciplineClassLocation>?) {
        if (locations == null) return
        database.classLocationDao().putSchedule(locations)
    }

    @WorkerThread
    private fun defineDisciplineGroups(groups: List<SDisciplineGroup>) {
        groups.forEach {
            database.classGroupDao().insert(it)
        }
    }

    @WorkerThread
    private fun defineDisciplines(disciplines: List<SDiscipline>) {
        val values = disciplines.map { Discipline.fromSagres(it) }
        database.disciplineDao().insert(values)
        disciplines.forEach { database.classDao().insert(it, true) }
    }

    @WorkerThread
    private fun defineCalendar(calendar: List<SCalendar>?) {
        val values = calendar?.map { CalendarItem.fromSagres(it) }
        database.calendarDao().deleteAndInsert(values)
    }

    private fun defineDemand(demandOpen: Boolean) {
        val flags = database.flagsDao().getFlagsDirect()
        if (flags == null) database.flagsDao().insertFlags(SagresFlags())

        database.flagsDao().updateDemand(demandOpen)

        if ((flags?.demandOpen == false || flags?.demandOpen == null) && demandOpen) {
            NotificationCreator.showDemandOpenNotification(context)
        }
    }

    private fun produceErrorMessage(callback: BaseCallback<*>) {
        Timber.e("Failed executing with status ${callback.status} and throwable message [${callback.throwable?.message}]")
    }

    @MainThread
    fun asyncSync() {
        executors.networkIO().execute { performSync("Manual") }
    }

    companion object {
        private val S_LOCK = Any()
    }
}