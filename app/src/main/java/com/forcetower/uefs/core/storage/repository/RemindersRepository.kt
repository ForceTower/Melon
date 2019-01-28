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
                    list.sortedWith(Comparator { o1, o2 ->
                        val date1 = o1.date
                        val date2 = o2.date
                        if (date1 != null && date2 != null) {
                            date1.compareTo(date2)
                        } else if (date1 != null && date2 == null) {
                            -1
                        } else if (date1 == null && date2 != null) {
                            1
                        } else {
                            val create1 = o1.createdAt
                            val create2 = o2.createdAt
                            create1?.compareTo(create2) ?: 1
                        }
                    })
                    data.postValue(list)
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
                .addOnCompleteListener(executors.networkIO(), OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("Updated reminder")
                    } else {
                        Timber.d("Reminder was not updated")
                        Timber.d("Exception message: ${task.exception?.message}")
                    }
                })
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