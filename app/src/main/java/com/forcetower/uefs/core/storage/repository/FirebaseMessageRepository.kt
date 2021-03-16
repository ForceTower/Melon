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
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkManager
import com.forcetower.sagres.SagresNavigator
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.notification.StatementNotificationProcessor
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.work.hourglass.HourglassContributeWorker
import com.forcetower.uefs.core.work.sync.SyncLinkedWorker
import com.forcetower.uefs.core.work.sync.SyncMainWorker
import com.forcetower.uefs.feature.shared.extensions.toBooleanOrNull
import com.forcetower.uefs.service.NotificationCreator
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMessageRepository @Inject constructor(
    private val service: UService,
    private val database: UDatabase,
    private val preferences: SharedPreferences,
    private val context: Context,
    private val syncRepository: SagresSyncRepository,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val firebaseMessaging: FirebaseMessaging,
    private val executors: AppExecutors
) {
    suspend fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        when {
            data.keys.isNotEmpty() -> onDataMessageReceived(data)
            message.notification != null -> onSimpleMessageReceived(message)
            else -> Timber.d("An invalid message was received")
        }
    }

    private suspend fun onDataMessageReceived(data: Map<String, String>) {
        Timber.d("Data message received")
        when (data["identifier"]) {
            "event" -> eventNotification(data)
            "teacher" -> teacherNotification(data)
            "remote_database" -> promoteDatabase(data)
            "service" -> serviceNotificationExtractor(data)
            "synchronize" -> universalSync()
            "reconnect_firebase" -> firebaseReconnect(data)
            "reschedule_sync" -> rescheduleSync(data)
            "hourglass_initiator" -> hourglassRunner()
            "worker_cancel" -> cancelWorker(data)
            "remote_preferences" -> promotePreferences(data)
            null -> Timber.e("Invalid notification received. No Identifier.")
        }
    }

    private fun serviceNotificationExtractor(data: Map<String, String>) {
        val typed = data["service_typed"]
        // Legacy notifications...
        if (typed == null) {
            serviceNotification(data)
        } else {
            when (typed) {
                "statement_received", "statement_received_hidden" -> statementReceivedBackbone(data)
            }
        }
    }

    private fun statementReceivedBackbone(data: Map<String, String>) {
        StatementNotificationProcessor.onStatementReceived(context, data)
    }

    private fun promotePreferences(data: Map<String, String>) {
        val type = data["type"]
        val key = data["key"]
        val value = data["value"]

        type ?: return
        key ?: return
        value ?: return

        val editor = preferences.edit()
        when (type) {
            "int" -> {
                val integer = value.toIntOrNull()
                if (integer != null) editor.putInt(key, integer)
            }
            "float" -> {
                val float = value.toFloatOrNull()
                if (float != null) editor.putFloat(key, float)
            }
            "boolean" -> {
                val bool = value.toBooleanOrNull()
                if (bool != null) editor.putBoolean(key, bool)
            }
            "long" -> {
                val long = value.toLongOrNull()
                if (long != null) editor.putLong(key, long)
            }
            "string" -> {
                editor.putString(key, value)
            }
        }

        editor.apply()
    }

    private fun cancelWorker(data: Map<String, String>) {
        val tag = data["tag"]
        tag ?: return
        WorkManager.getInstance(context).cancelAllWorkByTag(tag)
    }

    private fun hourglassRunner() {
        HourglassContributeWorker.createWorker(context)
    }

    private fun rescheduleSync(data: Map<String, String>) {
        val current = preferences.getString("stg_sync_frequency", "60")?.toIntOrNull() ?: 60
        val period = data["period"]?.toIntOrNull() ?: current
        val forced = data["forced"]?.toBooleanOrNull() ?: true

        /**
         * Se a frequencia atual for maior que a recomendada e a sincronização não for forçada,
         * nada precisa ser feito.
         *
         * Note que não é maior ou igual, este método se torna conveniente para que o remetente
         * possa enviar uma mensagem somente com o identificador reschedule_sync e o aparelho irá
         * fazer o reschedule mesmo que nenhum parâmetro seja passado.
         **/
        if (current > period && !forced) {
            Timber.d("No action needed")
        } else {
            when (preferences.getString("stg_sync_worker_type", "0")?.toIntOrNull() ?: 0) {
                0 -> SyncMainWorker.createWorker(context, period, true)
                1 -> {
                    SyncLinkedWorker.stopWorker(context)
                    SyncLinkedWorker.createWorker(context, period, true)
                }
            }
            preferences.edit().putString("stg_sync_frequency", period.toString()).apply()
            Timber.d("Target rescheduled")
        }
    }

    private fun firebaseReconnect(data: Map<String, String>) {
        // Esta função se tornou necessária ja que eu fiz a besteira de não colocar um toLowerCase nos nomes
        // que compõe as credenciais do firebase, isso poderia fazer com que todos os usuarios precisassem se reconectar.
        // Então, basta invocar esta função que a pessoa irá se reconectar aos serviços do firebase sem problemas.

        val unique = data["unique"]
        val version = data["version"]?.toIntOrNull()
        if (unique == null || version == null) {
            Timber.e("You need to specify a unique key and a version for this to work")
            return
        }

        val executed = preferences.getBoolean("${unique}__firebase", false)
        if (BuildConfig.VERSION_CODE >= version || executed) {
            Timber.d("Invalid version code($version) or executed($executed)")
            return
        }

        val completed = firebaseAuthRepository.reconnect()
        Timber.d("Finished execution >> Completed: $completed")
        preferences.edit().putBoolean("${unique}__firebase", completed).apply()
    }

    private suspend fun universalSync() {
        syncRepository.performSync("Universal")
    }

    private fun teacherNotification(data: Map<String, String>) {
        val message = data["message"]
        val teacher = data["teacher"]
        val discipline = data["discipline"]
        val timestamp = data["timestamp"]

        if (message == null || teacher == null || timestamp == null || discipline == null) {
            Timber.e("Invalid notification received. No message, teacher or timestamp")
            return
        }

        val sent = timestamp.toLongOrNull()
        if (sent == null) {
            Timber.e("Invalid notification received. Send time is invalid. Teacher: $teacher, $message")
            return
        }

        val default = Message(content = message, sagresId = System.currentTimeMillis(), notified = true, senderName = teacher, senderProfile = -2, timestamp = sent, discipline = discipline)
        val uid = database.messageDao().insert(default)
        NotificationCreator.showSagresMessageNotification(default, context, uid)
    }

    private fun serviceNotification(data: Map<String, String>) {
        val title = data["title"]
        val message = data["message"]?.replace("\\n", "\n")
        val image = data["image"]
        val institution = data["institution"]
        val course = data["course"]?.toLongOrNull()

        if (title == null || message == null) {
            Timber.e("Bad notification created. It was ignored")
            return
        }

        if (course != null) {
            val profile = database.profileDao().selectMeDirect()
            if (profile != null && profile.course != course) {
                return
            }
        }

        if (institution == null || institution == SagresNavigator.instance.getSelectedInstitution()) {
            NotificationCreator.showServiceMessageNotification(context, message.hashCode().toLong(), title, message, image)
        }
    }

    private fun eventNotification(data: Map<String, String>) {
        val id = data["eventId"]
        val title = data["title"]
        val description = data["description"]
        val image = data["image"]

        if (id == null || title == null || description == null) {
            Timber.e("Bad notification created. It was ignored")
            return
        }

        NotificationCreator.showEventNotification(context, id, title, description, image)
    }

    // This call is by far the most dangerous call that the hole code may have
    // Call allows the server to perform an update in the database at any moment
    // It will also be extremely helpful when something wrong happens and the server will be able to fix everyone at once
    private fun promoteDatabase(data: Map<String, String>) {
        val query = data["query"]
        val unique = data["unique"]

        if (unique != null) {
            val executed = preferences.getBoolean(unique, false)
            if (executed) {
                Timber.d("Promotion dismissed")
                return
            }
        }

        try {
            database.openHelper.writableDatabase.execSQL(query)
            if (unique != null)
                preferences.edit().putBoolean(unique, true).apply()
        } catch (t: Throwable) {
            Timber.d("Failed executing database promotion. ${t.message}")
            Timber.e(t)
        }
    }

    private fun onSimpleMessageReceived(message: RemoteMessage) {
        Timber.d("Simple notification received")
        val notification = message.notification
        if (notification == null) {
            Timber.e("Invalidation of notification happened really quickly")
            return
        }

        val content = notification.body
        val title = notification.title

        if (content == null || title == null) {
            Timber.e("Bad notification created. It was ignored")
            return
        }

        NotificationCreator.showSimpleNotification(context, title, content)
    }

    fun onNewToken(token: String) {
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

    fun subscribe(topics: Array<out String>) {
        executors.networkIO().execute {
            topics.map { firebaseMessaging.subscribeToTopic(it) }.forEach { task ->
                try {
                    Tasks.await(task)
                } catch (t: Throwable) {
                    Timber.e(t)
                }
            }
        }
    }

    @MainThread
    fun sendNewTokenOrNot(): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        executors.diskIO().execute {
            try {
                sendNewToken()
                result.postValue(true)
            } catch (t: Throwable) {
                result.postValue(false)
            }
        }
        return result
    }

    private fun sendNewToken() {
        val task = FirebaseMessaging.getInstance().token
        val value = Tasks.await(task)
        onNewToken(value)
    }
}
