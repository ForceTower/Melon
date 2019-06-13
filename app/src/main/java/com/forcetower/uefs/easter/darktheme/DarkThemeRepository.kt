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

package com.forcetower.uefs.easter.darktheme

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.crashlytics.android.Crashlytics
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.service.FirebaseProfile
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.easter.twofoureight.tools.ScoreKeeper
import com.forcetower.uefs.feature.shared.extensions.generateCalendarFromHour
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class DarkThemeRepository @Inject constructor(
    private val preferences: SharedPreferences,
    private val context: Context,
    private val functions: FirebaseFunctions,
    @Named(Profile.COLLECTION) private val collection: CollectionReference,
    private val firebaseAuth: FirebaseAuth,
    private val executors: AppExecutors,
    private val database: UDatabase
) {
    private lateinit var profileObserver: MutableLiveData<FirebaseProfile?>
    private var lastIteration: FirebaseProfile? = null

    fun getFirebaseProfile(): LiveData<FirebaseProfile?> {
        if (!::profileObserver.isInitialized) {
            profileObserver = MutableLiveData()
            val userId = firebaseAuth.currentUser?.uid
            if (userId == null) {
                profileObserver.value = null
            } else {
                collection.document(userId).addSnapshotListener { snapshot, exception ->
                    if (snapshot != null) {
                        val data = FirebaseProfile(
                                userId,
                                snapshot["course"] as? String,
                                snapshot["courseId"] as? Long,
                                snapshot["darkInvites"] as? Long,
                                snapshot["sentDarkInvites"] as? Long,
                                snapshot["darkThemeEnabled"] as? Boolean,
                                snapshot["email"] as? String,
                                snapshot["name"] as? String,
                                snapshot["username"] as? String ?: ""
                        )
                        profileObserver.value = data
                        lastIteration = data
                    }

                    if (exception != null) {
                        Crashlytics.logException(exception)
                    }
                }
            }
            return profileObserver
        } else {
            return profileObserver
        }
    }

    fun getPreconditions(firebaseProfile: FirebaseProfile? = lastIteration): LiveData<List<Precondition>> {
        val result = MutableLiveData<List<Precondition>>()
        executors.diskIO().execute {
            val precondition1 = create2048Precondition()
            val precondition2 = createLocationPrecondition()
            val precondition3 = createHoursPrecondition()
            val list = listOf(precondition1, precondition2, precondition3)
            executors.others().execute {
                if (firebaseProfile != null) sendInfoToFirebase(list, firebaseProfile)
            }
            result.postValue(list)
        }
        return result
    }

    @WorkerThread
    private fun sendInfoToFirebase(list: List<Precondition>, firebaseProfile: FirebaseProfile) {
        val uid = firebaseAuth.currentUser?.uid
        val completed = list.filter { it.completed }
        val completedSize = completed.size

        val enabled = preferences.getBoolean("ach_night_mode_enabled", false)

        val invites = if (completed.size < 2) 0 else (completedSize - 1)

        // TODO change this to new server
        val data = mutableMapOf<String, Any>()
        if (enabled) {
            preferences.edit()
                    .putInt("dark_theme_invites", invites)
                    .apply()
        } else {
            preferences.edit()
                    .putInt("dark_theme_invites", invites)
                    .putBoolean("ach_night_mode_enabled", (completedSize > 0))
                    .apply()
        }

        if (!enabled && completed.isEmpty()) return
        val serverEnabled = firebaseProfile.darkThemeEnabled ?: false
        val serverInvites = firebaseProfile.darkInvites ?: 0
        if (enabled == serverEnabled && serverInvites == invites.toLong()) return

        if (uid != null) {
            try {
                val actualInvites = max(serverInvites, invites.toLong())
                val dark = serverEnabled || (completedSize > 0)
                data += "darkThemeEnabled" to dark
                data += "darkInvites" to actualInvites
                Tasks.await(collection.document(uid).set(data, SetOptions.merge()))
            } catch (throwable: Throwable) {
                Crashlytics.logException(throwable)
            }
        }
    }

    private fun create2048Precondition(): Precondition {
        val the2048score = context.getSharedPreferences(ScoreKeeper.PREFERENCES, Context.MODE_PRIVATE)
                .getLong(ScoreKeeper.HIGH_SCORE, 0)
        Timber.d("2048 score: $the2048score")
        return Precondition(context.getString(R.string.precondition_1), context.getString(R.string.precondition_1_desc), the2048score >= 50000)
    }

    @WorkerThread
    private fun createLocationPrecondition(): Precondition {
        val bigTray = preferences.getBoolean("ach_dora_big_tray", false)
        val library = preferences.getBoolean("ach_dora_library", false)
        val zoology = preferences.getBoolean("ach_dora_zoology", false)
        val hogwarts = preferences.getBoolean("ach_dora_hogwarts", false)
        val module1 = preferences.getBoolean("ach_dora_mod1", false)
        val module7 = preferences.getBoolean("ach_dora_mod7", false)
        val management = preferences.getBoolean("ach_dora_management", false)
        val all = bigTray and library and zoology and hogwarts and module1 and module7 and management

        val schedule = database.classLocationDao().getCurrentScheduleDirect()
        var eightHours = false
        val day = schedule.groupBy { it.day }
        day.entries.forEach { group ->
            var minutes = 0
            group.value.forEach { location ->
                val start = location.startsAt.generateCalendarFromHour()?.timeInMillis
                val end = location.endsAt.generateCalendarFromHour()?.timeInMillis
                if (start != null && end != null) {
                    val diff = end - start
                    minutes += TimeUnit.MINUTES.convert(diff, TimeUnit.MILLISECONDS).toInt()
                }
            }
            if (minutes >= 480) eightHours = true
        }

        return Precondition(context.getString(R.string.precondition_2), context.getString(R.string.precondition_2_desc), all && eightHours)
    }

    @WorkerThread
    private fun createHoursPrecondition(): Precondition {
        val list = database.classDao().getAllDirect()
        val credits = list.asSequence().map { it.discipline().credits }.sum()
        Timber.d("Credits: $credits")
        return Precondition(context.getString(R.string.precondition_3), context.getString(R.string.precondition_3_desc, credits), credits >= 2200)
    }

    // TODO change this to the new server - The data of account must be updated after the send
    fun sendDarkThemeTo(username: String?): LiveData<Resource<Boolean>> {
        val result = MutableLiveData<Resource<Boolean>>()
        result.value = Resource.loading(false)
        functions.getHttpsCallable("sendDarkTheme").call(mapOf(
            "username" to username
        )).addOnCompleteListener {
            if (it.isSuccessful) {
                val sent = preferences.getInt("dark_theme_invites_sent", 0)
                preferences.edit().putInt("dark_theme_invites_sent", sent + 1).apply()
                result.postValue(Resource.success(true))
            } else {
                val exception = it.exception
                if (exception is FirebaseFunctionsException) {
                    val code = exception.code
                    Timber.d("Failed with code $code")
                    val message = exception.message
                    result.postValue(Resource.error(message ?: "Unknown", 400, exception))
                }
            }
        }
        return result
    }
}
