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
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.api.UResponse
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.model.unes.CreateStatementParams
import com.forcetower.uefs.core.model.unes.ProfileStatement
import com.forcetower.uefs.core.model.unes.SStudent
import com.forcetower.uefs.core.model.unes.SStudentDTO
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.network.adapter.asLiveData
import com.forcetower.uefs.core.storage.resource.NetworkBoundResource
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.work.statement.ProfileStatementWorker
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val database: UDatabase,
    private val executors: AppExecutors,
    private val service: UService,
    private val context: Context
) {
    fun getCommonProfile() = database.profileDao().selectMe()

    fun getMeProfileAsync() {
        executors.networkIO().execute {
            getMeProfileSync()
        }
    }

    @WorkerThread
    fun getMeProfileSync() {
        try {
            val response = service.getMeStudent().execute()
            val value = response.body()
            if (value?.data != null) {
                database.studentServiceDao().insertSingle(value.data.toCommon())
            }
        } catch (ignored: Throwable) {}
    }

    fun getMeProfile(): LiveData<Resource<SStudent>> {
        return object : NetworkBoundResource<SStudent, UResponse<SStudentDTO>>(executors) {
            override fun loadFromDb() = database.studentServiceDao().getMeStudent()
            override fun shouldFetch(it: SStudent?) = true
            override fun createCall() = service.getMeStudent().asLiveData()
            override fun saveCallResult(value: UResponse<SStudentDTO>) {
                if (value.data != null) {
                    Timber.d("Value returned data ${value.data}")
                    database.studentServiceDao().insertSingle(value.data.toCommon())
                }
            }
        }.asLiveData()
    }

    fun loadProfile(profileId: Long): LiveData<Resource<SStudent>> {
        return object : NetworkBoundResource<SStudent, UResponse<SStudentDTO>>(executors) {
            override fun loadFromDb() = database.studentServiceDao().getProfileById(profileId)
            override fun shouldFetch(it: SStudent?) = true
            override fun createCall() = service.getStudent(profileId).asLiveData()
            override fun saveCallResult(value: UResponse<SStudentDTO>) {
                if (value.data != null) {
                    database.studentServiceDao().insertSingle(value.data.toCommon())
                }
            }
        }.asLiveData()
    }

    fun getCourse(course: Long) = database.courseDao().getCourse(course)

    fun getProfileClasses() = database.classDao().getAll()

    fun updateUserCourse(course: Course) {
        executors.diskIO().execute {
            database.profileDao().updateCourse(course.id)
            val profile = database.profileDao().selectMeDirect() ?: return@execute
            try {
                service.setupProfile(profile).execute()
            } catch (t: Throwable) {}
        }
    }

    fun getAccountDatabase() = database.accountDao().getAccount()

    fun loadStatements(studentId: Long, userId: Long): LiveData<Resource<List<ProfileStatement>>> {
        return object : NetworkBoundResource<List<ProfileStatement>, UResponse<List<ProfileStatement>>>(executors) {
            override fun loadFromDb() = database.statementDao().getStatements(userId)
            override fun shouldFetch(it: List<ProfileStatement>?) = true
            // As of this version, the API uses the student id for loading the statements
            // This was a poor design on my part :/
            override fun createCall() = service.getStatements(studentId).asLiveData()
            override fun saveCallResult(value: UResponse<List<ProfileStatement>>) {
                if (value.data != null) {
                    database.statementDao().deleteAllFromReceiverId(userId)
                    database.statementDao().insert(value.data)
                }
            }
        }.asLiveData()
    }

    fun sendStatement(statement: String, profileId: Long, hidden: Boolean): LiveData<Resource<Boolean>> {
        val result = MutableLiveData<Resource<Boolean>>()
        executors.networkIO().execute {
            try {
                val map = CreateStatementParams(statement, hidden, profileId)
                val response = service.sendStatement(map).execute()
                if (response.isSuccessful) {
                    result.postValue(Resource.success(true))
                } else {
                    result.postValue(Resource.error(context.getString(R.string.write_statement_post_failed_code, response.code()), null))
                }
            } catch (error: Throwable) {
                Timber.e(error, "Error ${error.message}")
                result.postValue(Resource.error(context.getString(R.string.write_statement_network_error), null))
            }
        }
        return result
    }

    fun acceptStatementAsync(statement: ProfileStatement) {
        ProfileStatementWorker.createWorker(context, statement, ProfileStatementWorker.ACCEPT)
        executors.diskIO().execute { database.statementDao().markStatementAccepted(statement.id) }
    }

    fun refuseStatementAsync(statement: ProfileStatement) {
        ProfileStatementWorker.createWorker(context, statement, ProfileStatementWorker.REFUSE)
        executors.diskIO().execute { database.statementDao().markStatementRefused(statement.id) }
    }

    fun deleteStatementAsync(statement: ProfileStatement) {
        ProfileStatementWorker.createWorker(context, statement, ProfileStatementWorker.DELETE)
        executors.diskIO().execute { database.statementDao().markStatementDeleted(statement.id) }
    }

    @WorkerThread
    fun acceptStatement(statementId: Long): Int {
        val response = service.acceptStatement(mapOf("statement_id" to statementId)).execute()
        return if (response.isSuccessful) {
            database.statementDao().markStatementAccepted(statementId)
            0
        } else 1
    }

    @WorkerThread
    fun refuseStatement(statementId: Long): Int {
        val response = service.refuseStatement(mapOf("statement_id" to statementId)).execute()
        return if (response.isSuccessful) {
            database.statementDao().markStatementRefused(statementId)
            0
        } else 1
    }

    @WorkerThread
    fun deleteStatement(statementId: Long): Int {
        val response = service.refuseStatement(mapOf("statement_id" to statementId)).execute()
        return if (response.isSuccessful) {
            database.statementDao().markStatementDeleted(statementId)
            0
        } else 1
    }
}
