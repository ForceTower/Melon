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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SagresCredential
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.core.model.service.UMessage
import com.forcetower.uefs.core.model.unes.EdgeAppMessage
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.defineInDatabase
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.repository.CookieSessionRepository.Companion.INJECT_ERROR_NO_VALUE
import com.forcetower.uefs.core.storage.repository.CookieSessionRepository.Companion.INJECT_SUCCESS
import com.forcetower.uefs.core.task.definers.MessagesProcessor
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import dev.forcetower.breaker.Orchestra
import dev.forcetower.breaker.model.Authorization
import dev.forcetower.breaker.result.Outcome
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import timber.log.Timber

@Singleton
class MessagesRepository @Inject constructor(
    private val context: Context,
    private val client: OkHttpClient,
    private val database: UDatabase,
    private val cookieSessionRepository: CookieSessionRepository,
    @Named("flagSnowpiercerEnabled") private val snowpiercerEnabled: Boolean,
    @Named("webViewUA") private val agent: String
) {
    fun getMessages(): Flow<PagingData<Message>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { database.messageDao().getAllMessagesPaged() }
    ).flow

    fun fetchMessages(fetchAll: Boolean = false): LiveData<Boolean> {
        return if (snowpiercerEnabled) {
            fetchMessagesSnowflake(fetchAll).asLiveData(Dispatchers.IO)
        } else {
            fetchMessagesCase(fetchAll).asLiveData(Dispatchers.IO)
        }
    }

    private fun fetchMessagesSnowflake(fetchAll: Boolean) = flow {
        val access = database.accessDao().getAccessDirect()
        val profile = database.profileDao().selectMeDirect()
        if (access == null || profile == null) {
            emit(false)
        } else {
            val orchestra = Orchestra.Builder().client(client).userAgent(agent).build()
            orchestra.setAuthorization(Authorization(access.username, access.password))

            val outcome = orchestra.messages(profile.sagresId, amount = if (fetchAll) 0 else 10)
            if (outcome is Outcome.Success) {
                MessagesProcessor(outcome.value, database, context, true).execute()
                emit(true)
            } else {
                emit(false)
            }
        }
    }

    private fun fetchMessagesCase(all: Boolean = false) = flow {
        val profile = database.profileDao().selectMeDirect()
        val access = database.accessDao().getAccessDirect()
        if (profile != null && access != null) {
            SagresNavigator.instance.putCredentials(SagresCredential(access.username, access.password, SagresNavigator.instance.getSelectedInstitution()))
            val messages = if (!profile.mocked) {
                SagresNavigator.instance.messages(profile.sagresId, all)
            } else {
                val me = SagresNavigator.instance.me()
                val person = me.person
                if (person != null) {
                    database.profileDao().insert(person)
                    SagresNavigator.instance.messages(person.id, all)
                } else {
                    val injected = cookieSessionRepository.injectGoodCookiesOnClient()
                    if (injected == INJECT_SUCCESS) {
                        SagresNavigator.instance.messagesHtml()
                    } else {
                        if (injected == INJECT_ERROR_NO_VALUE) {
                            database.accessDao().setAccessValidationSuspend(false)
                            Timber.d("User didn't have a injectable cookie... Logout actually :D")
                        }
                        emit(false)
                        return@flow
                    }
                }
            }

            Timber.d("Profile mocked: ${profile.mocked}, ${profile.sagresId}, $all")

            if (messages.status == Status.SUCCESS) {
                Timber.d("${messages.messages}")
                messages.messages.defineInDatabase(database, true)
                emit(true)
            } else {
                Timber.d("${messages.status}")
                emit(false)
            }
        } else {
            emit(false)
        }
    }

    fun getUnesMessages(): Flow<List<EdgeAppMessage>> {
        return database.edgeMessages.getAll()
    }
}
