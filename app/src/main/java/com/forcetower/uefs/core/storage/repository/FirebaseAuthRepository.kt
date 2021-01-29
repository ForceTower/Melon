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

import android.content.Context
import android.content.SharedPreferences
import com.forcetower.sagres.database.model.SagresPerson
import com.forcetower.sagres.utils.WordUtils
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.model.unes.Profile
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber
import java.util.Locale
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

    fun loginToFirebase(person: SagresPerson, access: Access, reconnect: Boolean = false) {
        if (reconnect) { firebaseAuth.signOut() }
        if (firebaseAuth.currentUser == null) {
            val user = access.username.toLowerCase(Locale.getDefault())
            val username = if (user.contains("@")) {
                "${user.substring(0, user.indexOf("@"))}_email"
            } else {
                user
            }

            val email = context.getString(R.string.email_unes_format, username)

            val profiler = "__${username}__"
            val password = context.getString(R.string.firebase_password_pattern, username, profiler, secret)

            attemptSignIn(
                email,
                context.getString(R.string.firebase_password_pattern, username, password, secret),
                access,
                person
            )
        }
    }

    private fun attemptSignIn(email: String, password: String, access: Access, person: SagresPerson) {
        Timber.d("Attempt Login")
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(
                executors.others(),
                { task ->
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
                }
            )
    }

    private fun attemptCreateAccount(email: String, password: String, access: Access, person: SagresPerson) {
        Timber.d("Attempt Create account")
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(
                executors.others(),
                { task ->
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
                }
            )
    }

    private fun connected(access: Access, person: SagresPerson, uid: String) {
        Timber.d("Creating student profile for ${person.name?.trim()} UID: $uid")

        val data = mutableMapOf(
            "name" to WordUtils.toTitleCase(person.name?.trim()),
            "username" to access.username,
            "email" to (person.email?.trim()?.toLowerCase(Locale.getDefault()) ?: "unknown@unes.com"),
            "cpf" to person.getCpf()?.trim(),
            "sagresId" to person.id,
            "imageUrl" to "/users/$uid/avatar.jpg",
            "manufacturer" to android.os.Build.MANUFACTURER,
            "model" to android.os.Build.MODEL
        )

        val idTask = FirebaseMessaging.getInstance().token
        try {
            val result = Tasks.await(idTask)
            preferences.edit().putString("current_firebase_token", result).apply()
            data["firebaseToken"] = result
        } catch (t: Throwable) {
            Timber.e(t)
        }

        userCollection.document(uid).set(data, SetOptions.merge())
            .addOnCompleteListener(
                executors.others(),
                OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("User data set!")
                    } else {
                        Timber.d("Failed to set data...")
                        Timber.d("Exception: ${task.exception}")
                    }
                }
            )
    }

    fun updateCourse(course: Course, user: FirebaseUser) {
        val data = mapOf(
            "courseId" to course.id,
            "course" to course.name
        )
        userCollection.document(user.uid).set(data, SetOptions.merge())
            .addOnCompleteListener(
                executors.others(),
                OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("User course data set!")
                    } else {
                        Timber.d("Failed to set course data...")
                        Timber.d("Exception: ${task.exception}")
                    }
                }
            )
    }

    fun updateFrequency(value: Int) {
        val user = firebaseAuth.currentUser
        user ?: return

        val data = mapOf("syncFrequency" to value)
        userCollection.document(user.uid).set(data, SetOptions.merge())
            .addOnCompleteListener(
                executors.others(),
                OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("Completed setting frequency")
                    } else {
                        Timber.d("Failed to set frequency")
                        Timber.d("Exception: ${task.exception}")
                    }
                }
            )
    }

    fun reconnect(): Boolean {
        firebaseAuth.signOut()
        return true
    }
}
