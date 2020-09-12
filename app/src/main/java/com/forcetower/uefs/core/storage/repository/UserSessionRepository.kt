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

import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import com.forcetower.uefs.core.model.service.UserSessionDTO
import com.forcetower.uefs.core.model.unes.UserSession
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.repository.cloud.AuthRepository
import timber.log.Timber
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionRepository @Inject constructor(
    private val database: UDatabase,
    private val service: UService,
//    private val executors: AppExecutors,
//    private val preferences: SharedPreferences,
    private val authRepository: AuthRepository
) {

    @WorkerThread
    fun onSessionStarted(): UserSession {
        val uuid = UUID.randomUUID().toString()
        val now = Calendar.getInstance().timeInMillis
        val session = UserSession(uuid, now)
        database.userSessionDao().insert(session)
        return session
    }

    @WorkerThread
    fun onUserInteraction() {
        val now = Calendar.getInstance().timeInMillis
        val session = database.userSessionDao().getLatestSession() ?: onSessionStarted()
        val lastInteraction = session.lastInteraction ?: 0
        val difference = now - lastInteraction
        if (difference >= 45000 && session.lastInteraction != null) {
            onSessionStarted()
        } else {
            database.userSessionDao().updateLastInteraction(session.uid, now)
        }
    }

    @WorkerThread
    fun onUserClickedAd() {
        val session = database.userSessionDao().getLatestSession() ?: onSessionStarted()
        database.userSessionDao().updateClickedAd(session.uid, 1)
    }

    @WorkerThread
    fun onUserAdImpression() {
        val session = database.userSessionDao().getLatestSession() ?: onSessionStarted()
        database.userSessionDao().updateAdImpression(session.uid, 1)
    }

    @WorkerThread
    fun syncSessions() {
        Timber.d("Started sync session...")
        val sessions = database.userSessionDao().getUnsyncedSessions()
        if (sessions.isEmpty()) {
            Timber.d("All sessions in sync...")
            return
        }

        Timber.d("Session sync will be performed")

        val start = sessions.map { it.started }.minOrNull() ?: 0
        val end = sessions.map { it.started }.maxOrNull() ?: 0
        val dto = UserSessionDTO(start, end, sessions)
        try {
            val response = service.saveSessions(dto).execute()
            if (response.isSuccessful) {
                sessions.forEach { database.userSessionDao().markSyncedSession(it.uid) }
                Timber.d("Sessions sync completed")
                database.userSessionDao().removeSyncedSessions()
            } else {
                Timber.d("Response failed with ${response.code()}")
                // User is not authorized...
                // Reconnect...
                if (response.code() == 401) {
                    Timber.d("User needs to reconnect...")
                    database.accessTokenDao().deleteAll()
                    authRepository.performAccountSyncState()
                }
            }
        } catch (error: Throwable) {
            Timber.e(error, "It seems that the sync failed")
        }
    }

    @AnyThread
    fun onUserInteractionAsync() {
//        if (!preferences.isStudentFromUEFS()) return
//        executors.diskIO().execute { onUserInteraction() }
    }

    @AnyThread
    fun onSessionStartedAsync() {
//        if (!preferences.isStudentFromUEFS()) return
//        executors.diskIO().execute { onSessionStarted() }
    }

    @AnyThread
    fun onUserClickedAdAsync() {
//        if (!preferences.isStudentFromUEFS()) return
//        executors.diskIO().execute { onUserClickedAd() }
    }

    @AnyThread
    fun onUserAdImpressionAsync() {
//        if (!preferences.isStudentFromUEFS()) return
//        executors.diskIO().execute { onUserAdImpression() }
    }

    @AnyThread
    fun onSyncSessionsAsync() {
        // executors.networkIO().execute { syncSessions() }
    }
}
