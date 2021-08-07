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

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.util.round
import com.google.firebase.auth.FirebaseAuth
import dev.forcetower.breaker.Orchestra
import dev.forcetower.breaker.model.Authorization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SagresDataRepository @Inject constructor(
    private val database: UDatabase,
    private val executor: AppExecutors,
    private val firebaseAuth: FirebaseAuth,
    private val preferences: SharedPreferences,
    private val client: OkHttpClient,
    @Named("webViewUA") private val agent: String,
    private val cookies: CookieSessionRepository
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
            val classes = database.classDao().getAllDirect().filter { it.clazz.finalScore != null }
            val hours = classes.sumOf { it.discipline.credits }
            val mean = classes.sumOf {
                val zeroValue = it.clazz.missedClasses > (it.discipline.credits / 4)
                val finalScore = if (zeroValue) 0.0 else it.clazz.finalScore!!
                it.discipline.credits * finalScore
            }
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

    fun attemptLoginWithNewPassword(password: String, token: String?): LiveData<Resource<Boolean>> {
        return liveData(Dispatchers.IO) {
            val access = database.accessDao().getAccessDirect()
            if (access == null) {
                emit(Resource.error("", false))
            } else {
                emit(Resource.loading(false))
                val callback = SagresNavigator.instance.login(access.username, access.password, token)
                if (callback.status == Status.INVALID_LOGIN) {
                    emit(Resource.success(false))
                } else {
                    cookies.findAndSaveCookies()
                    database.accessDao().run {
                        setAccessValidationSuspend(true)
                        updateAccessPasswordSuspend(password)
                    }
                    emit(Resource.success(true))
                }
            }
        }
    }

    suspend fun loginWithNewPasswordSuspend(password: String): Boolean = withContext(Dispatchers.IO) {
        val access = database.accessDao().getAccessDirectSuspend() ?: return@withContext false
        val orchestra = Orchestra.Builder().client(client).userAgent(agent).build()
        orchestra.setAuthorization(Authorization(access.username, password))
        try {
            val outcome = orchestra.login()

            if (outcome.isSuccess) {
                database.accessDao().run {
                    setAccessValidationSuspend(true)
                    updateAccessPasswordSuspend(password)
                }
            }

            outcome.isSuccess
        } catch (error: Throwable) {
            Timber.e(error, "Error during password change")
            false
        }
    }

    fun getScheduleHideCount(): LiveData<Int> {
        return database.classLocationDao().getHiddenClassesCount()
    }
}
