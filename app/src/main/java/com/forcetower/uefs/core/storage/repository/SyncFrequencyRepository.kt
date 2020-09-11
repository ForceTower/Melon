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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.uefs.core.model.service.SyncFrequency
import com.google.firebase.firestore.CollectionReference
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SyncFrequencyRepository @Inject constructor(
    @Named(SyncFrequency.COLLECTION)
    private val collection: CollectionReference
) {

    fun getFrequencies(): LiveData<List<SyncFrequency>> {
        val result = MutableLiveData<List<SyncFrequency>>()
        collection.addSnapshotListener { snapshot, exception ->
            when {
                snapshot != null -> {
                    val frequencies = snapshot.documents
                        .map { it.toObject(SyncFrequency::class.java)!! }
                        .sortedBy { it.value }
                        .toMutableList()
                    if (frequencies.isEmpty()) { frequencies += SyncFrequency() }
                    result.postValue(frequencies)
                }
                exception != null -> {
                    Timber.d("Exception: ${exception.message}")
                    Timber.e(exception)
                    result.postValue(listOf(SyncFrequency()))
                }
                else -> {
                    Timber.e("Something really odd happened")
                    result.postValue(listOf(SyncFrequency()))
                }
            }
        }
        return result
    }
}
