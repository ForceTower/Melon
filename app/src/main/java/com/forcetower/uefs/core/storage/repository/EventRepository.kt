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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.uefs.core.model.unes.Event
import com.google.firebase.firestore.CollectionReference
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class EventRepository @Inject constructor(
    @Named(Event.COLLECTION) private val collection: CollectionReference
) {
    fun getCurrentEvents(): LiveData<List<Event>> {
        val data = MutableLiveData<List<Event>>()
        collection.whereEqualTo("approved", true).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val result = task.result
                if (result == null) {
                    Timber.d("Events Task result is null")
                    data.postValue(emptyList())
                } else {
                    val list = result.documents.map { it.toObject(Event::class.java)!!.copy(id = it.id) }
                    Timber.d("Event List: $list")
                    data.postValue(list)
                }
            } else {
                Timber.d("Unsuccessful task. Exception ${task.exception?.message}")
                data.postValue(emptyList())
            }
        }
        return data
    }

    fun getEvents(): LiveData<List<Event>> {
        val data = MutableLiveData<List<Event>>()
        collection.whereEqualTo("approved", true).addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Timber.d("Exception on document read")
            } else {
                if (snapshot != null) {
                    val list = snapshot.documents.map { it.toObject(Event::class.java)!! }
                    data.postValue(list)
                } else {
                    data.postValue(emptyList())
                }
            }
        }
        return data
    }
}
