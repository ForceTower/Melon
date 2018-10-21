/*
 * Copyright (c) 2018.
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

package com.forcetower.uefs.architecture.service

import android.content.SharedPreferences
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.storage.repository.FirebaseMessageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.android.AndroidInjection
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class FirebaseActionsService: FirebaseMessagingService() {
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    @Inject @Named(Profile.COLLECTION)
    lateinit var profileCollection: CollectionReference
    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var repository: FirebaseMessageRepository

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        message?: return
        repository.onMessageReceived(message)
    }

    override fun onNewToken(token: String?) {
        token?: return
        val user = firebaseAuth.currentUser
        if (user != null) {
            val data = mapOf("firebaseToken" to token)
            profileCollection.document(user.uid).set(data, SetOptions.merge()).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.d("Completed")
                } else {
                    Timber.d("Failed with exception message: ${task.exception?.message}")
                }
            }
        } else {
            Timber.d("Disconnected")
        }

        preferences.edit().putString("current_firebase_token", token).apply()
    }
}