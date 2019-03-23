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

import android.content.SharedPreferences
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.storage.database.UDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @Named(Profile.COLLECTION)
    private val profileReference: CollectionReference,
    private val firebaseAuth: FirebaseAuth,
    private val preferences: SharedPreferences,
    private val executors: AppExecutors,
    private val database: UDatabase,
    private val gradesRepository: SagresGradesRepository,
    private val adventureRepository: AdventureRepository
) {

    @MainThread
    fun hasDarkModeEnabled(): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        val default = preferences.getBoolean("ach_night_mode_enabled", false)
        val uid = firebaseAuth.currentUser?.uid
        result.value = default
//        if (uid != null) {
//            profileReference.document(uid).addSnapshotListener { snapshot, exception ->
//                if (snapshot != null) {
//                    val enabled = snapshot["darkThemeEnabled"] as? Boolean ?: false
//                    preferences.edit().putBoolean("ach_night_mode_enabled", enabled).apply()
//                    result.postValue(enabled)
//                } else {
//                    Crashlytics.logException(exception)
//                    result.postValue(default)
//                }
//            }
//        } else {
//            result.value = default
//        }

        return result
    }

    @AnyThread
    fun requestAllGradesAndCalculateScore() {
        executors.networkIO().execute {
            var loginNeeded = true
            val semesters = database.semesterDao().getSemestersDirect()
            semesters.forEach {
                val result = gradesRepository.getGrades(it.sagresId, loginNeeded)
                loginNeeded = false
                if (result != 0) {
                    Timber.d("Failed to run on semester ${it.sagresId} - ${it.codename}: $result")
                }
            }
            adventureRepository.performCheckAchievements(HashMap())
        }
    }
}
