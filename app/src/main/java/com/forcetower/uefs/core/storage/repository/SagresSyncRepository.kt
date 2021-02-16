/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
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
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import com.forcetower.core.getDynamicDataSourceFactory
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SagresCalendar
import com.forcetower.sagres.database.model.SagresCredential
import com.forcetower.sagres.database.model.SagresDiscipline
import com.forcetower.sagres.database.model.SagresDisciplineClassLocation
import com.forcetower.sagres.database.model.SagresDisciplineGroup
import com.forcetower.sagres.database.model.SagresDisciplineMissedClass
import com.forcetower.sagres.database.model.SagresGrade
import com.forcetower.sagres.database.model.SagresMessage
import com.forcetower.sagres.database.model.SagresPerson
import com.forcetower.sagres.database.model.SagresRequestedService
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
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.repository.cloud.AuthRepository
import com.forcetower.uefs.core.util.LocationShrinker
import com.forcetower.uefs.core.util.VersionUtils
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.core.work.discipline.DisciplinesDetailsWorker
import com.forcetower.uefs.core.work.hourglass.HourglassContributeWorker
import com.forcetower.uefs.service.NotificationCreator
import com.google.android.gms.tasks.Tasks
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
    private val service: UService,
    private val remoteConfig: FirebaseRemoteConfig,
    private val preferences: SharedPreferences
) {
    private val mutex = Mutex()

    private suspend fun findAndMatch() {
        val aeri = getDynamicDataSourceFactory(context, "com.forcetower.uefs.aeri.domain.AERIDataSourceFactoryProvider")
        aeri?.create()?.run {
            update()
            getNotifyMessages().forEach {
                NotificationCreator.showSimpleNotification(context, it.title, it.content)
            }
        }
    }

    @WorkerThread
    suspend fun performSync(executor: String, gToken: String? = null) = withContext(Dispatchers.IO) {
        val registry = createRegistry(executor)
        val access = database.accessDao().getAccessDirect()
        access ?: Timber.d("Access is null, sync will not continue")
        if (access != null) {
            FirebaseCrashlytics.getInstance().setUserId(access.username)
            // Only one sync may be active at a time
            mutex.withLock {
                execute(access, registry, executor, gToken)
            }
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
                    manager?.connectionInfo?.ssid ?: "Unknown"
                } else {
                    val manager = context.getSystemService(TelephonyManager::class.java)
                    manager?.simOperatorName ?: "Operator"
                }
                Timber.d("Is on Wifi? $wifi. Network name: $network")
                SyncRegistry(
                    executor = executor,
                    network = network,
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
    private suspend fun execute(access: Access, registry: SyncRegistry, executor: String, gToken: String?) {
        val uid = database.syncRegistryDao().insert(registry)
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        registry.uid = uid
        SagresNavigator.instance.putCredentials(SagresCredential(access.username, access.password, SagresNavigator.instance.getSelectedInstitution()))

        // Internal checks for canceling auto sync
        // this was useful to avoid unes from ddos'ing the website.
        // they said unes was doing it anyways, so here is a deleted useless piece of code
        // you welcome
        if (!Constants.EXECUTOR_WHITELIST.contains(executor.toLowerCase(Locale.getDefault()))) {
            Timber.d("There was a time where this would cause sync to be aborted if server.. But i don't care anymore")
        }

        try {
            findAndMatch()
        } catch (t: Throwable) { }

        database.gradesDao().markAllNotified()
        database.messageDao().setAllNotified()
        database.classMaterialDao().markAllNotified()

        val homeDoc = when {
            preferences.isStudentFromUEFS() && gToken == null -> initialPage()
            !preferences.isStudentFromUEFS() || gToken != null -> login(access, gToken)
            else -> null
        }
        val score = SagresBasicParser.getScore(homeDoc)
        Timber.d("Login Completed. Score Parsed: $score")

        // Since stuff is just broken....
        if (homeDoc == null) {
            registry.completed = true
            registry.error = -2
            registry.success = false
            registry.message = "Login falhou"
        } else {
            defineSchedule(SagresScheduleParser.getSchedule(homeDoc))
            defineMessages(SagresMessageParser.getMessages(homeDoc))
        }

        val person = me(score, access)
        Timber.d("The person from me is ${person?.name} ${person?.isMocked}")
        if (person == null) {
            registry.completed = true
            registry.error = -3
            registry.success = false
            registry.message = "The dream is over"
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
                Timber.e(t)
            }
        }

        var result = 0
        var skipped = 0

        if (!person.isMocked) {
            Timber.d("I guess the person is not a mocked version on it")
            if (!messages(person.id)) {
                if (homeDoc != null && !messages(null)) result += 1 shl 1
            }
            if (!semesters(person.id)) result += 1 shl 2
            if (homeDoc == null) {
                registry.completed = true
                registry.error = 10
                registry.success = true
                registry.message = "Partial sync"
                registry.executor = "${registry.executor}-Parc"
                registry.end = System.currentTimeMillis()
                database.syncRegistryDao().update(registry)
                return
            }
        } else {
            if (!messages(null)) result += 1 shl 1
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
            if (!preferences.getBoolean("sent_hourglass_testing_data_0.0.2", false) &&
                authRepository.getAccessTokenDirect() != null
            ) {
                HourglassContributeWorker.createWorker(context)
                preferences.edit().putBoolean("sent_hourglass_testing_data_0.0.2", true).apply()
            }
        }

        try {
            val day = preferences.getInt("sync_daily_update", -1)
            if (day != today) {
                adventureRepository.performCheckAchievements(HashMap())

                val task = FirebaseMessaging.getInstance().token
                val value = Tasks.await(task)
                onNewToken(value)

                preferences.edit().putInt("sync_daily_update", today).apply()
            }
            createNewVersionNotification()
        } catch (t: Throwable) {
            Timber.e(t)
        }

        registry.completed = true
        registry.error = result
        registry.success = result == 0
        registry.skipped = skipped
        registry.message = "Deve-se consultar as flags de erro"
        registry.end = System.currentTimeMillis()
        database.syncRegistryDao().update(registry)
    }

    private fun defineMessages(values: List<SagresMessage>) {
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

    private fun initialPage(): Document? {
        val document = SagresNavigator.instance.startPage()
        when (document.status) {
            Status.SUCCESS -> return document.document
            else -> produceErrorMessage(document)
        }
        return null
    }

    fun login(access: Access, gToken: String?): Document? {
        val login = SagresNavigator.instance.login(access.username, access.password, gToken)
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
        if (
            access != null &&
            access.valid &&
            com.forcetower.sagres.Constants.getParameter("REQUIRES_CAPTCHA") != "true"
        ) {
            database.accessDao().setAccessValidation(false)
            NotificationCreator.showInvalidAccessNotification(context)
        }
    }

    private fun me(score: Double, access: Access): SagresPerson? {
        val username = access.username
        if (username.contains("@")) {
            return continueWithHtml(username, score)
        } else {
            val me = SagresNavigator.instance.me()
            Timber.d("Me response: ${me.status}")
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
                Status.RESPONSE_FAILED, Status.NETWORK_ERROR -> {
                    return continueWithHtml(username, score)
                }
                else -> produceErrorMessage(me)
            }
        }
        return null
    }

    private fun continueWithHtml(username: String, score: Double): SagresPerson? {
        val start = SagresNavigator.instance.startPage().document ?: return null
        val name = SagresBasicParser.getName(start) ?: username
        val person = SagresPerson(username.hashCode().toLong(), name, name, "00000000000", username).apply { isMocked = true }
        database.profileDao().insert(person, score)
        return person
    }

    @WorkerThread
    private fun messages(userId: Long?): Boolean {
        Timber.d("Messages was invoked using $userId")
        val messages = if (userId != null)
            SagresNavigator.instance.messages(userId)
        else
            SagresNavigator.instance.messagesHtml()

        Timber.d("Did receive a valid list? ${messages.messages != null}, ${messages.status}")

        return when (messages.status) {
            Status.SUCCESS -> {
                val values = messages.messages?.map { Message.fromMessage(it, false) } ?: emptyList()
                Timber.d("Messages mapped: ${values.size}")
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
            getAllUnnotified().forEach {
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

    private fun defineServices(services: List<SagresRequestedService>?) {
        services ?: return
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
    private fun defineFrequency(frequency: List<SagresDisciplineMissedClass>?) {
        frequency ?: return
        database.classAbsenceDao().putAbsences(frequency)
    }

    @WorkerThread
    private fun defineGrades(grades: List<SagresGrade>?) {
        grades ?: return
        database.gradesDao().putGrades(grades)
    }

    @WorkerThread
    private fun defineSemesters(semesters: List<Pair<Long, String>>?) {
        semesters?.forEach {
            val semester = Semester(sagresId = it.first, name = it.second, codename = it.second)
            database.semesterDao().insertIgnoring(semester)
        }
    }

    @WorkerThread
    private fun defineSchedule(locations: List<SagresDisciplineClassLocation>?) {
        locations ?: return
        val ordering = preferences.getBoolean("stg_semester_deterministic_ordering", true)
        val shrinkSchedule = preferences.getBoolean("stg_schedule_shrinking", true)
        if (shrinkSchedule) {
            val shrink = LocationShrinker.shrink(locations)
            database.classLocationDao().putSchedule(shrink, ordering)
        } else {
            database.classLocationDao().putSchedule(locations, ordering)
        }
    }

    @WorkerThread
    private fun defineDisciplineGroups(groups: List<SagresDisciplineGroup>?) {
        groups ?: return
        database.classGroupDao().defineGroups(groups)
    }

    @WorkerThread
    private fun defineDisciplines(disciplines: List<SagresDiscipline>?) {
        disciplines ?: return
        val values = disciplines.map { Discipline.fromSagres(it) }
        database.disciplineDao().insert(values)
        disciplines.forEach { database.classDao().insert(it, true) }
    }

    @WorkerThread
    private fun defineCalendar(calendar: List<SagresCalendar>?) {
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
        Timber.d("Is throwable invalid? ${callback.throwable == null}")
        callback.throwable?.printStackTrace()
        Timber.e("Failed executing with status ${callback.status} and throwable message [${callback.throwable?.message}]")
    }

    suspend fun asyncSync(gToken: String?) {
        performSync("Manual", gToken)
    }

    companion object {
        private val S_LOCK = Any()
    }
}
