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
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.service.Reminder
import com.forcetower.uefs.core.model.unes.Profile
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RemindersRepository @Inject constructor(
    private val executors: AppExecutors,
    private val firebaseAuth: FirebaseAuth,
    @Named(Profile.COLLECTION) private val profileReference: CollectionReference
) {

    fun getReminders(): LiveData<List<Reminder>> {
        val data = MutableLiveData<List<Reminder>>()
        val user = firebaseAuth.currentUser
        if (user != null) {
            profileReference.document(user.uid).collection(Reminder.COLLECTION).orderBy("createdAt", Query.Direction.DESCENDING).addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.map { it.toObject(Reminder::class.java)!!.apply { id = it.id } }
                    val deadline = list.filter { it.date != null }.sortedBy { it.date!! }
                    val common = list.filter { it.date == null }
                    val result = deadline + common
                    data.postValue(result)
                }
            }
        } else {
            Timber.d("Not connected")
        }
        return data
    }

    fun createReminder(reminder: Reminder) {
        val user = firebaseAuth.currentUser
        if (user != null) {
            profileReference.document(user.uid).collection(Reminder.COLLECTION).add(reminder).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.d("Created")
                } else {
                    Timber.d("Creation failed")
                    Timber.d("Exception message: ${task.exception?.message}")
                }
            }
        } else {
            Timber.d("Not connected")
        }
    }

    fun updateReminderCompleteStatus(reminder: Reminder, next: Boolean) {
        val user = firebaseAuth.currentUser
        if (user != null) {
            Timber.d("Updating reminder: ${reminder.id} :: ${reminder.title}")
            val data = mapOf("completed" to next)
            profileReference.document(user.uid).collection(Reminder.COLLECTION).document(reminder.id)
                .set(data, SetOptions.merge())
                .addOnCompleteListener(
                    executors.networkIO(),
                    OnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Timber.d("Updated reminder")
                        } else {
                            Timber.d("Reminder was not updated")
                            Timber.d("Exception message: ${task.exception?.message}")
                        }
                    }
                )
        } else {
            Timber.d("Not connected")
        }
    }

    fun deleteReminder(reminder: Reminder) {
        val user = firebaseAuth.currentUser
        if (user != null) {
            profileReference.document(user.uid).collection(Reminder.COLLECTION).document(reminder.id)
                .delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("Deleted reminder")
                    } else {
                        Timber.d("Reminder was not deleted")
                        Timber.d("Exception message: ${task.exception?.message}")
                    }
                }
        } else {
            Timber.d("Not connected")
        }
    }
}
