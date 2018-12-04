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

package com.forcetower.uefs.core.storage.repository

import android.content.Context
import android.content.SharedPreferences
import com.forcetower.sagres.database.model.SPerson
import com.forcetower.sagres.utils.WordUtils
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.model.unes.Profile
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @Named(Profile.COLLECTION) private val userCollection: CollectionReference,
    private val context: Context,
    private val executors: AppExecutors,
    private val preferences: SharedPreferences
) {
    private val secret = context.getString(R.string.firebase_account_secret)

    fun loginToFirebase(person: SPerson, access: Access, reconnect: Boolean = false) {
        val email = context.getString(R.string.email_unes_format, access.username.toLowerCase())

        val profiler = "${person.sagresId}__${access.username.toLowerCase()}__${access.password.toLowerCase()}"
        val username = access.username.toLowerCase()
        val password = context.getString(R.string.firebase_password_pattern, username, profiler, secret)

        if (reconnect) { firebaseAuth.signOut() }

        attemptSignIn(email, context.getString(R.string.firebase_password_pattern, username, password, secret), access, person)
    }

    private fun attemptSignIn(email: String, password: String, access: Access, person: SPerson) {
        Timber.d("Attempt Login")
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(executors.others(), OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        if (user == null) {
                            attemptCreateAccount(email, password, access, person)
                        } else {
                            Timber.d("Connected! Your account is: $user")
                            connected(access, person, user.uid)
                        }
                    } else {
                        Timber.d("Failed to Sign In...")
                        Timber.d("Exception: ${task.exception}")
                        attemptCreateAccount(email, password, access, person)
                    }
                })
    }

    private fun attemptCreateAccount(email: String, password: String, access: Access, person: SPerson) {
        Timber.d("Attempt Create account")
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(executors.others(), OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        if (user == null) {
                            Timber.d("Failed anyways")
                        } else {
                            Timber.d("Connected! Your account is: $user")
                            connected(access, person, user.uid)
                        }
                    } else {
                        Timber.d("Failed to Create account...")
                        Timber.d("Exception: ${task.exception}")
                    }
                })
    }

    private fun connected(access: Access, person: SPerson, uid: String) {
        Timber.d("Creating student profile for ${person.name.trim()} UID: $uid")

        val data = mutableMapOf(
                "name" to WordUtils.capitalize(person.name.trim()),
                "username" to access.username,
                "email" to person.email.trim().toLowerCase(),
                "cpf" to person.cpf.trim(),
                "sagresId" to person.id,
                "imageUrl" to "/users/$uid/avatar.jpg",
                "manufacturer" to android.os.Build.MANUFACTURER,
                "model" to android.os.Build.MODEL
        )

        val token = preferences.getString("current_firebase_token", null)
        if (token != null) {
            data["firebaseToken"] = token
        }

        userCollection.document(uid).set(data, SetOptions.merge())
                .addOnCompleteListener(executors.others(), OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("User data set!")
                    } else {
                        Timber.d("Failed to set data...")
                        Timber.d("Exception: ${task.exception}")
                    }
                })
    }

    fun updateCourse(course: Course, user: FirebaseUser) {
        val data = mapOf(
                "courseId" to course.id,
                "course" to course.name
        )
        userCollection.document(user.uid).set(data, SetOptions.merge())
                .addOnCompleteListener(executors.others(), OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("User course data set!")
                    } else {
                        Timber.d("Failed to set course data...")
                        Timber.d("Exception: ${task.exception}")
                    }
                })
    }

    fun updateFrequency(value: Int) {
        val user = firebaseAuth.currentUser
        user ?: return

        val data = mapOf("syncFrequency" to value)
        userCollection.document(user.uid).set(data, SetOptions.merge())
                .addOnCompleteListener(executors.others(), OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("Completed setting frequency")
                    } else {
                        Timber.d("Failed to set frequency")
                        Timber.d("Exception: ${task.exception}")
                    }
                })
    }

    fun reconnect(): Boolean {
        firebaseAuth.signOut()
        return true
    }
}