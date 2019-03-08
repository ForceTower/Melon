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
import com.forcetower.sagres.SagresNavigator
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.util.truncate
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SagresDataRepository @Inject constructor(
    private val database: UDatabase,
    private val executor: AppExecutors,
    private val firebaseAuth: FirebaseAuth,
    private val preferences: SharedPreferences
) {
    fun getMessages() = database.messageDao().getAllMessages()

    fun logout() {
        executor.diskIO().execute {
            firebaseAuth.signOut()
            preferences.edit().remove("hourglass_status").apply()
            database.accessDao().deleteAll()
            database.accessTokenDao().deleteAll()
            database.profileDao().deleteMe()
            database.classDao().deleteAll()
            database.semesterDao().deleteAll()
            database.messageDao().deleteAll()
            database.demandOfferDao().deleteAll()
            database.serviceRequestDao().deleteAll()
            SagresNavigator.instance.logout()
        }
    }

    fun getFlags() = database.flagsDao().getFlags()
    fun getSemesters() = database.semesterDao().getParticipatingSemesters()
    fun getCourse() = database.profileDao().getProfileCourse()

    fun lightweightCalcScore() {
        executor.diskIO().execute {
            val classes = database.classDao().getAllDirect()
            val hours = classes.filter { it.clazz.finalScore != null }.sumBy { it.discipline().credits }
            val mean = classes.filter { it.clazz.finalScore != null }
                    .sumByDouble { it.discipline().credits * it.clazz.finalScore!! }
            if (hours > 0) {
                val score = (mean / hours).truncate()
                Timber.d("Score is $score")
                database.profileDao().updateCalculatedScore(score)
            }
        }
    }
}
