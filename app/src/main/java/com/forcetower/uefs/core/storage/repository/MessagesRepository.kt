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

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SagresCredential
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.service.UMessage
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.defineInDatabase
import com.forcetower.uefs.core.storage.database.UDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class MessagesRepository @Inject constructor(
    private val database: UDatabase,
    private val executors: AppExecutors,
    @Named(UMessage.COLLECTION) private val collection: CollectionReference
) {
    fun getMessages(): LiveData<PagedList<Message>> {
        return LivePagedListBuilder(database.messageDao().getAllMessagesPaged(), 20).build()
    }

    fun fetchMessages(fetchAll: Boolean = false): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        executors.networkIO().execute {
            val bool = fetchMessagesCase(fetchAll)
            result.postValue(bool)
        }
        return result
    }

    @WorkerThread
    fun fetchMessagesCase(all: Boolean = false): Boolean {
        val profile = database.profileDao().selectMeDirect()
        val access = database.accessDao().getAccessDirect()
        return if (profile != null && access != null) {
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
                    SagresNavigator.instance.login(access.username, access.password)
                    SagresNavigator.instance.messagesHtml()
                }
            }

            Timber.d("Profile mocked: ${profile.mocked}, ${profile.sagresId}, $all")

            if (messages.status == Status.SUCCESS) {
                Timber.d("${messages.messages}")
                messages.messages.defineInDatabase(database, true)
                true
            } else {
                Timber.d("${messages.status}")
                false
            }
        } else {
            false
        }
    }

    fun getUnesMessages(): LiveData<List<UMessage>> {
        val result = MutableLiveData<List<UMessage>>()
        val institution = SagresNavigator.instance.getSelectedInstitution()
        collection.orderBy("createdAt", Query.Direction.DESCENDING).addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Timber.e(exception)
            } else if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(UMessage::class.java)?.apply {
                    id = it.id
                    val replaced = message.replace("\\n", "\n")
                    message = replaced
                } }.filter { it.institution == null || it.institution == institution }
                result.postValue(list)
            }
        }
        return result
    }
}