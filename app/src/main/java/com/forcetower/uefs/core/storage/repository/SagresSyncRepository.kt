/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
import com.forcetower.sagres.database.model.SMessage
import com.forcetower.sagres.database.model.SPerson
import com.forcetower.sagres.database.model.SRequestedService
import com.forcetower.sagres.operation.BaseCallback
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.parsers.SagresBasicParser
import com.forcetower.sagres.parsers.SagresMessageParser
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
import com.forcetower.uefs.core.model.unes.SagresFlags
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.model.unes.ServiceRequest
import com.forcetower.uefs.core.model.unes.SyncRegistry
import com.forcetower.uefs.core.model.unes.notify
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.APIService
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.repository.cloud.AuthRepository
import com.forcetower.uefs.core.util.VersionUtils
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.core.work.discipline.DisciplinesDetailsWorker
import com.forcetower.uefs.core.work.hourglass.HourglassContributeWorker
import com.forcetower.uefs.service.NotificationCreator
import com.google.android.gms.tasks.Tasks
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.jsoup.nodes.Document
import timber.log.Timber
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SagresSyncRepository @Inject constructor(
    private val context: Context,
    private val database: UDatabase,
    private val executors: AppExecutors,
    private val authRepository: AuthRepository,
    private val adventureRepository: AdventureRepository,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val syncService: APIService,
    private val service: UService,
    private val remoteConfig: FirebaseRemoteConfig,
    private val preferences: SharedPreferences
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
                SyncRegistry(executor = executor, network = operatorName ?: "invalid", networkType = NetworkType.CELLULAR.ordinal)
            } else {
                SyncRegistry(executor = executor, network = info.ssid, networkType = NetworkType.WIFI.ordinal)
            }
        }
    }

    @WorkerThread
    private fun execute(access: Access, registry: SyncRegistry, executor: String) {
        val uid = database.syncRegistryDao().insert(registry)
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        registry.uid = uid

        if (!Constants.EXECUTOR_WHITELIST.contains(executor.toLowerCase(Locale.getDefault()))) {
            try {
                val call = syncService.getUpdate()
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
                Timber.e(t, "An error just happened... It will complete anyways")
            }
        }

        database.gradesDao().markAllNotified()
        database.messageDao().setAllNotified()
        database.classMaterialDao().markAllNotified()
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
        defineMessages(SagresMessageParser.getMessages(homeDoc))

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
        var skipped = 0

        if (!messages(null))

        if (!person.isMocked) {
            if (!messages(person.id)) result += 1 shl 1
            if (!semesters(person.id)) result += 1 shl 2
        }

        val dailyDisciplines = preferences.getString("stg_daily_discipline_sync", "2")?.toIntOrNull() ?: 2
        val currentDaily = preferences.getInt("daily_discipline_count", 0)
        val currentDayDiscipline = preferences.getInt("daily_discipline_day", -1)
        val lastDailyHour = preferences.getInt("daily_discipline_hour", 0)
        val isNewDaily = currentDayDiscipline != today || dailyDisciplines == -1
        val currentDailyHour = calendar.get(Calendar.HOUR_OF_DAY)

        val (actualDailyCount, nextHour) = if (isNewDaily)
            0 to -1
        else
            currentDaily to if (lastDailyHour < 8) 10 else lastDailyHour + 4

        val shouldDisciplineSync =
            ((actualDailyCount < dailyDisciplines) || (dailyDisciplines == -1)) &&
            (currentDailyHour >= nextHour)

        Timber.d("Discipline Sync Dump >> will sync now $shouldDisciplineSync")
        Timber.d("Dailies $dailyDisciplines")
        Timber.d("Current daily $currentDaily")
        Timber.d("Current Day discipline $currentDayDiscipline")
        Timber.d("Last daily hour $lastDailyHour")
        Timber.d("Is this a new daily? $isNewDaily")
        Timber.d("Current hour $currentDailyHour")
        Timber.d("Actual daily count $actualDailyCount")
        Timber.d("Next daily hour $nextHour")

        if (shouldDisciplineSync) {
            if (!disciplinesExperimental()) result += 1 shl 6
            else {
                preferences.edit()
                    .putInt("daily_discipline_count", actualDailyCount + 1)
                    .putInt("daily_discipline_day", today)
                    .putInt("daily_discipline_hour", currentDailyHour)
                    .apply()
            }
        } else {
            skipped += 1 shl 1
        }

        if (!startPage()) result += 1 shl 3
        if (!grades()) result += 1 shl 4
        if (!servicesRequest()) result += 1 shl 5

        val uefsStudent = preferences.isStudentFromUEFS()
        if (uefsStudent) {
            serviceLogin()
        }

        if (preferences.getBoolean("primary_fetch", true)) {
            DisciplinesDetailsWorker.createWorker(context)
            preferences.edit().putBoolean("primary_fetch", false).apply()
        }

        if (uefsStudent) {
            if (!preferences.getBoolean("sent_hourglass_testing_data_0.0.1", false) &&
                    authRepository.getAccessTokenDirect() != null) {
                HourglassContributeWorker.createWorker(context)
                preferences.edit().putBoolean("sent_hourglass_testing_data_0.0.1", true).apply()
            }
        }

        try {
            val day = preferences.getInt("sync_daily_update", -1)
            if (day != today) {
                adventureRepository.performCheckAchievements(HashMap())

                val task = FirebaseInstanceId.getInstance().instanceId
                val value = Tasks.await(task)
                onNewToken(value.token)

                preferences.edit().putInt("sync_daily_update", today).apply()
            }
            createNewVersionNotification()
        } catch (t: Throwable) {
            Crashlytics.logException(t)
        }

        registry.completed = true
        registry.error = result
        registry.success = result == 0
        registry.skipped = skipped
        registry.message = "Deve-se consultar as flags de erro"
        registry.end = System.currentTimeMillis()
        database.syncRegistryDao().update(registry)
    }

    private fun defineMessages(values: List<SMessage>) {
        values.reversed().forEachIndexed { index, message ->
            message.processingTime = System.currentTimeMillis() + index
        }
        database.messageDao().insertIgnoring(values.map { Message.fromMessage(it, false) })
    }

    private fun onNewToken(token: String) {
        val auth = database.accessTokenDao().getAccessTokenDirect()
        if (auth != null) {
            try {
                service.sendToken(mapOf("token" to token)).execute()
            } catch (t: Throwable) { }
        } else {
            Timber.d("Disconnected")
        }
        preferences.edit().putString("current_firebase_token", token).apply()
    }

    private fun serviceLogin() {
        val token = authRepository.getAccessTokenDirect()
        if (token == null || !preferences.getBoolean("__reconnect_account_for_name_update__", false)) {
            executors.networkIO().execute {
                authRepository.performAccountSyncState()
            }
            preferences.edit().putBoolean("__reconnect_account_for_name_update__", true).apply()
        }
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
                if (login.code == 401) {
                    onInvalidLogin()
                }
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
            return continueWithHtml(document, username, score)
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
                    return continueWithHtml(document, username, score)
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
                Timber.d("Disciplines: ${start.disciplines}")
                Timber.d("Groups: ${start.groups}")
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
    private fun disciplinesExperimental(): Boolean {
        Timber.d("Experimental Experimental Start")
        val experimental = SagresNavigator.instance.disciplinesExperimental()
        return when (experimental.status) {
            Status.COMPLETED -> {
                Timber.d("Experimental Completed")
                defineSemesters(experimental.getSemesters())
                defineDisciplines(experimental.getDisciplines())
                defineDisciplineGroups(experimental.getGroups())

                materialsNotifications()

                Timber.d("Semesters: ${experimental.getSemesters()}")
                Timber.d("Disciplines: ${experimental.getDisciplines()}")
                Timber.d("Groups: ${experimental.getGroups()}")
                true
            }
            else -> {
                Timber.d("Experimental Failed")
                produceErrorMessage(experimental)
                false
            }
        }
    }

    @WorkerThread
    private fun materialsNotifications() {
        database.classMaterialDao().run {
            getAllUnnotified().filter { it.group() != null }.forEach {
                NotificationCreator.showMaterialPostedNotification(context, it)
            }
            markAllNotified()
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
        database.classGroupDao().defineGroups(groups)
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