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
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.forcetower.sagres.Constants
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SagresDisciplineMissedClass
import com.forcetower.sagres.database.model.SagresGrade
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.task.definers.DisciplinesProcessor
import dev.forcetower.breaker.Orchestra
import dev.forcetower.breaker.model.Authorization
import dev.forcetower.breaker.result.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class SagresGradesRepository @Inject constructor(
    private val context: Context,
    private val database: UDatabase,
    private val executors: AppExecutors,
    private val client: OkHttpClient,
    @Named("webViewUA") private val agent: String,
    @Named("flagSnowpiercerEnabled") private val snowpiercer: Boolean
) {
    @AnyThread
    fun getGradesAsync(semesterId: Long, needLogin: Boolean): LiveData<Int> {
        return if (snowpiercer) {
            getGradesSnowflake(semesterId).asLiveData(Dispatchers.IO)
        } else {
            val data = MutableLiveData<Int>()
            executors.networkIO().execute {
                val result = getGrades(semesterId, needLogin)
                data.postValue(result)
            }
            data
        }
    }

    private fun getGradesSnowflake(semesterId: Long) = flow {
        val access = database.accessDao().getAccessDirect()
        val profile = database.profileDao().selectMeDirect()
        if (access == null || profile == null) {
            emit(NO_ACCESS)
        } else {
            val orchestra = Orchestra.Builder().client(client).userAgent(agent).build()
            orchestra.setAuthorization(Authorization(access.username, access.password))
            val outcome = orchestra.grades(profile.sagresId, semesterId)
            if (outcome is Outcome.Success) {
                val currentSemester = database.semesterDao().getSemesterDirect(semesterId)
                DisciplinesProcessor(context, database, outcome.value, currentSemester!!.uid, profile.uid, false).execute()
                emit(SUCCESS)
            } else {
                emit(CURRENT_GRADES_FAILED)
            }
        }
    }

    @WorkerThread
    fun getGrades(semesterSagresId: Long, needLogin: Boolean = true): Int {
        val access = database.accessDao().getAccessDirect()
        access ?: return NO_ACCESS

        return if (needLogin) {
            if (Constants.getParameter("REQUIRES_CAPTCHA") != "true") {
                val login = SagresNavigator.instance.login(access.username, access.password)
                if (login.status == Status.SUCCESS && login.document != null) {
                    Timber.d("[$semesterSagresId] Login Completed Correctly")
                    proceed(semesterSagresId)
                } else {
                    INVALID_ACCESS
                }
            } else {
                proceed(semesterSagresId)
            }
        } else {
            proceed(semesterSagresId)
        }
    }

    @WorkerThread
    private fun proceed(semesterSagresId: Long): Int {
        val grades = SagresNavigator.instance.getCurrentGrades()
        return if (grades.status == Status.SUCCESS && grades.document != null) {
            Timber.d("[$semesterSagresId] Grades Part 01/02 Completed!")
            val semesterGrades = SagresNavigator.instance.getGradesFromSemester(semesterSagresId, grades.document!!)
            if (semesterGrades.status == Status.SUCCESS) {
                defineSemesters(semesterGrades.semesters)
                defineGrades(semesterGrades.grades)
                defineFrequency(semesterGrades.frequency)
                Timber.d("[$semesterSagresId] Grades Part 02/02 Completed!")
                Timber.d("[$semesterSagresId] Grades: ${semesterGrades.grades}")
                SUCCESS
            } else {
                ACTUAL_GRADES_CALL_FAILED
            }
        } else {
            Timber.d("Current Grades Status Failed")
            CURRENT_GRADES_FAILED
        }
    }

    @WorkerThread
    private fun defineFrequency(frequency: List<SagresDisciplineMissedClass>?) {
        if (frequency == null) return
        database.classAbsenceDao().putAbsences(frequency)
    }

    @WorkerThread
    private fun defineGrades(grades: List<SagresGrade>?) {
        grades ?: return
        database.gradesDao().putGrades(grades, notify = false)
    }

    @WorkerThread
    private fun defineSemesters(semesters: List<Pair<Long, String>>?) {
        semesters?.forEach {
            val semester = Semester(sagresId = it.first, name = it.second, codename = it.second)
            database.semesterDao().insertIgnoring(semester)
        }
    }

    companion object {
        const val SUCCESS = 0
        const val NO_ACCESS = -1
        const val INVALID_ACCESS = -2
        const val CURRENT_GRADES_FAILED = -3
        const val ACTUAL_GRADES_CALL_FAILED = -4
    }
}
