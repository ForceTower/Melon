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
import android.net.Uri
import androidx.lifecycle.LiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.siecomp.ServerSession
import com.forcetower.uefs.core.model.siecomp.Speaker
import com.forcetower.uefs.core.model.unes.AccessToken
import com.forcetower.uefs.core.storage.eventdatabase.EventDatabase
import com.forcetower.uefs.core.storage.eventdatabase.accessors.SessionWithData
import com.forcetower.uefs.core.storage.imgur.ImageUploader
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.resource.NetworkBoundResource
import com.forcetower.uefs.service.NotificationCreator
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SIECOMPRepository @Inject constructor(
    private val database: EventDatabase,
    private val executors: AppExecutors,
    private val service: UService,
    private val context: Context,
    private val client: OkHttpClient
) {
    fun getSessionsFromDayLocal(day: Int) = database.eventDao().getSessionsFromDay(day)

    fun getAllSessions() =
        object : NetworkBoundResource<List<SessionWithData>, List<ServerSession>>(executors) {
            override fun loadFromDb() = database.eventDao().getAllSessions()
            override fun shouldFetch(it: List<SessionWithData>?) = true
            override fun createCall() = service.siecompSessions()
            override fun saveCallResult(value: List<ServerSession>) {
                database.eventDao().insertServerSessions(value)
            }
        }.asLiveData()

    fun getSessionDetails(id: Long): LiveData<SessionWithData> {
        return database.eventDao().getSessionWithId(id)
    }

    fun getSpeaker(speakerId: Long): LiveData<Speaker> {
        return database.eventDao().getSpeakerWithId(speakerId)
    }

    fun markSessionStar(sessionId: Long, star: Boolean) {
        executors.diskIO().execute {
            database.eventDao().markSessionStar(sessionId, star)
        }
    }

    fun loginToService(username: String, password: String) {
        executors.networkIO().execute {
            try {
                val response = service.login(username, password).execute()
                if (response.isSuccessful) {
                    val token = response.body()!!
                    Timber.d("Token: $token")
                    database.accessTokenDao().deleteAll()
                    database.accessTokenDao().insert(token)
                    NotificationCreator.showSimpleNotification(context, "Login Concluido", "Você agr tem acesso a funções exclusivas")
                } else {
                    NotificationCreator.showSimpleNotification(context, "Login falhou", "O login retornou com o código ${response.code()}")
                    Timber.e(response.message())
                }
            } catch (t: Throwable) {
                Timber.e("Exception@${t.message}")
            }
        }
    }

    fun sendSpeaker(speaker: Speaker, create: Boolean) {
        executors.networkIO().execute {
            try {
                Timber.d("Speaker $speaker")
                val image = speaker.image
                if (image != null && !image.contains("imgur.com")) {
                    Timber.d("Uploading image")
                    val uri = Uri.parse(image)
                    val link = ImageUploader.uploadToImGur(uri, context, client)?.link
                    if (link == null) {
                        Timber.d("Image not uploaded... unsetting...")
                    } else {
                        Timber.d("Image uploaded... setting")
                    }
                    speaker.image = link
                }

                val response = if (create) {
                    service.createSpeaker(speaker).execute()
                } else {
                    service.updateSpeaker(speaker).execute()
                }
                if (response.isSuccessful) {
                    NotificationCreator.showSimpleNotification(context, "Operação concluida", "A requisição concluiu com sucesso")
                } else {
                    NotificationCreator.showSimpleNotification(context, "Operação falhou", "A operação falhou com o código ${response.code()}")
                    Timber.e(response.message())
                }
            } catch (t: Throwable) {
                Timber.e("Connection failed")
            }
        }
    }

    fun getAccess(): LiveData<AccessToken?> {
        return database.accessTokenDao().getAccess()
    }
}
