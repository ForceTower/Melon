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
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.model.unes.NetworkType
import com.forcetower.uefs.core.model.unes.SyncRegistry
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.task.definers.DisciplinesProcessor
import com.forcetower.uefs.core.task.definers.LectureProcessor
import com.forcetower.uefs.core.task.definers.MessagesProcessor
import com.forcetower.uefs.core.task.definers.MissedLectureProcessor
import com.forcetower.uefs.core.task.definers.SemestersProcessor
import com.forcetower.uefs.core.util.VersionUtils
import com.forcetower.uefs.service.NotificationCreator
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.forcetower.breaker.Orchestra
import dev.forcetower.breaker.model.Authorization
import dev.forcetower.breaker.model.Person
import dev.forcetower.breaker.result.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SnowpiercerSyncRepository @Inject constructor(
    client: OkHttpClient,
    @Named("webViewUA") agent: String,
    private val context: Context,
    private val database: UDatabase,
    private val preferences: SharedPreferences
) {
    private val orchestra = Orchestra.Builder().client(client).userAgent(agent).build()

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
            // The first request failed... stop here
            return
        }

        val person = (login as Outcome.Success).value
        val localProfileId = defineUser(person)

        val messagesOutcome = orchestra.messages(person.id)
        (messagesOutcome as? Outcome.Success)?.let { success ->
            val page = success.value
            MessagesProcessor(page, database, context, false).execute()
        }

        if (messagesOutcome is Outcome.Error) {
            Timber.d("Messages error code: ${messagesOutcome.code}")
            Timber.e(messagesOutcome.error, "Failed to execute grades")
        }

        val semestersOutcome = orchestra.semesters(person.id)
        val currentSemester = (semestersOutcome as? Outcome.Success)?.let { success ->
            val semesters = success.value
            // if this all works, migrate date parsing into the Snowpiercer
            SemestersProcessor(database, semesters).execute()

            val current = semesters.maxByOrNull { ZonedDateTime.parse(it.start, DateTimeFormatter.ISO_OFFSET_DATE_TIME) }
            current
        }

        if (semestersOutcome is Outcome.Error) {
            Timber.d("Semester error code: ${semestersOutcome.code}")
            Timber.e(semestersOutcome.error, "Failed to execute semesters")
        }

        // if no current semester... back off
        currentSemester?.let { semester ->
            val gradesOutcome = orchestra.grades(person.id, semester.id)
            (gradesOutcome as? Outcome.Success)?.let { success ->
                val disciplines = success.value
                val currentSemesterIns = database.semesterDao().getSemesterDirect(semester.id)!!
                DisciplinesProcessor(context, database, disciplines, currentSemesterIns.uid, localProfileId, true).execute()

                if (shouldUpdateDisciplines()) {
                    val classes = disciplines.flatMap { it.classes }

                    classes.map { clazz -> clazz.id to orchestra.lectures(clazz.id, 0, 0) }
                        .forEach { pair ->
                            val (id, outcome) = pair
                            if (outcome is Outcome.Success) {
                                val group = database.classGroupDao().getByElementalIdDirect(id)
                                if (group != null) {
                                    LectureProcessor(context, database, group.uid, outcome.value, true).execute()
                                }
                            }
                        }

                    classes.map { clazz -> clazz.id to orchestra.absences(person.id, clazz.id, 0, 0) }
                        .forEach { pair ->
                            val (id, outcome) = pair
                            if (outcome is Outcome.Success) {
                                val group = database.classGroupDao().getByElementalIdDirect(id)
                                if (group != null) {
                                    MissedLectureProcessor(context, database, localProfileId, group.uid, outcome.value, true).execute()
                                }
                            }
                        }
                }
            }

            if (gradesOutcome is Outcome.Error) {
                Timber.d("Grades error code: ${gradesOutcome.code}")
                Timber.e(gradesOutcome.error, "Failed to execute grades")
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

    private suspend fun defineUser(person: Person): Long {
        return database.profileDao().insert(person)
    }

    private fun onAccessInvalided() {
        database.accessDao().setAccessValidation(false)
        NotificationCreator.showInvalidAccessNotification(context)
    }

    private suspend fun findAndMatch() {
        val aeri = getDynamicDataSourceFactory(context, "com.forcetower.uefs.aeri.domain.AERIDataSourceFactoryProvider") ?: return
        val source = aeri.create()
        source.update()
        val messages = source.getNotifyMessages()
        messages.forEach {
            NotificationCreator.showAERIMessageNotification(context, it.content.hashCode().toLong(), it.title, it.content, it.imageUrl)
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

    private fun shouldUpdateDisciplines(): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)

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

        val execute = ((actualDailyCount < dailyDisciplines) || (dailyDisciplines == -1)) &&
            (currentDailyHour >= nextHour)

        if (execute) {
            preferences.edit()
                .putInt("daily_discipline_count", actualDailyCount + 1)
                .putInt("daily_discipline_day", today)
                .putInt("daily_discipline_hour", currentDailyHour)
                .apply()
        }
        return execute
    }

    private fun produceErrorMessage(outcome: Outcome.Error<*>) {
        Timber.e(outcome.error, "Failed executing with status ${outcome.code} and throwable message [${outcome.error.message}]")
    }

    suspend fun asyncSync() = withContext(Dispatchers.IO) {
        performSync("Manual")
    }
}
