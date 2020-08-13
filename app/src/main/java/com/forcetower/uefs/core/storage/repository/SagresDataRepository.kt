/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.sagres.Constants
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.operation.login.LoginCallback
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.util.round
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
            preferences.edit()
                    .remove("hourglass_status")
                    .apply()
            database.accessDao().deleteAll()
            database.accessTokenDao().deleteAll()
            database.accountDao().deleteAll()
            database.profileDao().deleteMe()
            database.classDao().deleteAll()
            database.semesterDao().deleteAll()
            database.messageDao().deleteAll()
            database.demandOfferDao().deleteAll()
            database.serviceRequestDao().deleteAll()
            SagresNavigator.instance.logout()
            SagresNavigator.instance.putCredentials(null)
        }
    }

    fun getFlags() = database.flagsDao().getFlags()
    fun getSemesters() = database.semesterDao().getParticipatingSemesters()
    fun getCourse() = database.profileDao().getProfileCourse()

    fun lightweightCalcScore() {
        executor.diskIO().execute {
            val classes = database.classDao().getAllDirect()
            val hours = classes.filter { it.clazz.finalScore != null }.sumBy { it.discipline.credits }
            val mean = classes.filter { it.clazz.finalScore != null }
                    .sumByDouble { it.discipline.credits * it.clazz.finalScore!! }
            if (hours > 0) {
                val score = (mean / hours).round(1)
                Timber.d("Score is $score")
                database.profileDao().updateCalculatedScore(score)
            }
        }
    }

    fun changeAccessValidation(valid: Boolean) {
        executor.diskIO().execute {
            database.accessDao().setAccessValidation(valid)
        }
    }

    // TODO [REQUIRES PATCHING SAVID-1]
    // TODO This one actually requires a new login, so must show captcha (for now)
    // TODO Use this for captcha invalidation as well! Cool!
    fun attemptLoginWithNewPassword(password: String): LiveData<Resource<Boolean>> {
        val result = MutableLiveData<Resource<Boolean>>()
        executor.networkIO().execute {
            val access = database.accessDao().getAccessDirect()
            if (access == null) {
                result.postValue(Resource.error("", false))
            } else {
                result.postValue(Resource.loading(null))
                val callback = if (Constants.getParameter("REQUIRES_CAPTCHA") != "true") {
                    SagresNavigator.instance.login(access.username, access.password)
                } else {
                    // TODO Change this
                    LoginCallback(Status.INVALID_LOGIN)
                }
                if (callback.status == Status.INVALID_LOGIN) {
                    result.postValue(Resource.success(false))
                } else {
                    database.accessDao().run {
                        setAccessValidation(true)
                        updateAccessPassword(password)
                    }
                    result.postValue(Resource.success(true))
                }
            }
        }
        return result
    }

    fun getScheduleHideCount(): LiveData<Int> {
        return database.classLocationDao().getHiddenClassesCount()
    }
}
