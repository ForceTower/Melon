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

import android.os.Build
import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.service.Feedback
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.feature.shared.extensions.toBase64
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class FeedbackRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val executors: AppExecutors,
    private val database: UDatabase,
    @Named(Feedback.COLLECTION)
    private val collection: CollectionReference
) {

    @AnyThread
    fun onSendFeedback(text: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        executors.diskIO().execute {
            val userId = firebaseAuth.currentUser?.uid
            val access = database.accessDao().getAccessDirect()
                ?: Access(username = "random", password = "user")
            val profile = database.profileDao().selectMeDirect()
            val feedback = Feedback(
                text = text,
                username = access.username,
                course = profile?.course,
                email = profile?.email,
                hash = access.toString().toBase64(),
                firebaseId = userId,
                manufacturer = Build.MANUFACTURER,
                deviceModel = Build.MODEL,
                android = Build.VERSION.SDK_INT
            )
            var token: String? = null
            try {
                token = Tasks.await(FirebaseMessaging.getInstance().token)
            } catch (throwable: Throwable) {
                Timber.e(throwable)
            }

            feedback.currentToken = token

            try {
                Tasks.await(collection.add(feedback))
            } catch (throwable: Throwable) {
                Timber.e(throwable)
            }
        }
        result.postValue(true)
        return result
    }
}
