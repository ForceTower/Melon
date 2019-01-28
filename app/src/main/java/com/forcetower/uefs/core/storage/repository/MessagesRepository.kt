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

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.forcetower.sagres.SagresNavigator
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
        return if (profile != null) {
            val messages = SagresNavigator.instance.messages(profile.sagresId, all)
            if (messages.status == Status.SUCCESS) {
                messages.messages.defineInDatabase(database, true)
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    fun getUnesMessages(): LiveData<List<UMessage>> {
        val result = MutableLiveData<List<UMessage>>()
        collection.orderBy("createdAt", Query.Direction.DESCENDING).addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Timber.e(exception)
            } else if (snapshot != null) {
                val list = snapshot.documents.map { it.toObject(UMessage::class.java)!!.apply {
                    id = it.id
                    val replaced = message.replace("\\n", "\n")
                    message = replaced
                } }
                result.postValue(list)
            }
        }
        return result
    }
}