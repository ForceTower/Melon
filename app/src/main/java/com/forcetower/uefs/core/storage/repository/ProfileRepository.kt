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

import androidx.lifecycle.LiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.api.UResponse
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.model.unes.SStudent
import com.forcetower.uefs.core.model.unes.SStudentDTO
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.network.adapter.asLiveData
import com.forcetower.uefs.core.storage.resource.NetworkBoundResource
import com.forcetower.uefs.core.storage.resource.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val database: UDatabase,
    private val executors: AppExecutors,
    private val service: UService
) {
    fun getMeProfile(): LiveData<Resource<SStudent>> {
        return object : NetworkBoundResource<SStudent, UResponse<SStudentDTO>>(executors) {
            override fun loadFromDb() = database.studentServiceDao().getMeStudent()
            override fun shouldFetch(it: SStudent?) = true
            override fun createCall() = service.getMeStudent().asLiveData()
            override fun saveCallResult(value: UResponse<SStudentDTO>) {
                if (value.data != null)
                    database.studentServiceDao().insertSingle(value.data.toCommon())
            }
        }.asLiveData()
    }

    fun loadProfile(profileId: Long): LiveData<Resource<SStudent>> {
        return object : NetworkBoundResource<SStudent, UResponse<SStudentDTO>>(executors) {
            override fun loadFromDb() = database.studentServiceDao().getProfileById(profileId)
            override fun shouldFetch(it: SStudent?) = true
            override fun createCall() = service.getStudent(profileId).asLiveData()
            override fun saveCallResult(value: UResponse<SStudentDTO>) {
                if (value.data != null)
                    database.studentServiceDao().insertSingle(value.data.toCommon())
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
}