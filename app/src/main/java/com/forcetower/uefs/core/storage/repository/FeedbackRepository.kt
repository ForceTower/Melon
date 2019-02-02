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

import android.os.Build
import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.crashlytics.android.Crashlytics
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.service.Feedback
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.feature.shared.extensions.toBase64
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.iid.FirebaseInstanceId
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
                    manufacturer = android.os.Build.MANUFACTURER,
                    deviceModel = android.os.Build.MODEL,
                    android = Build.VERSION.SDK_INT
            )
            var token: String? = null
            try {
                token = Tasks.await(FirebaseInstanceId.getInstance().instanceId).token
            } catch (throwable: Throwable) {
                Crashlytics.logException(throwable)
            }

            feedback.currentToken = token

            try {
                Tasks.await(collection.add(feedback))
                result.postValue(true)
            } catch (throwable: Throwable) {
                Crashlytics.logException(throwable)
                result.postValue(false)
            }
        }
        return result
    }
}
