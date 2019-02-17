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

package com.forcetower.uefs.core.storage.repository.api

import androidx.lifecycle.LiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.api.UDiscipline
import com.forcetower.uefs.core.model.api.helpers.UHourOverview
import com.forcetower.uefs.core.model.api.helpers.UResponse
import com.forcetower.uefs.core.storage.apidatabase.APIDatabase
import com.forcetower.uefs.core.storage.network.APIService
import com.forcetower.uefs.core.storage.repository.DisciplineDetailsRepository

import com.forcetower.uefs.core.storage.resource.NetworkBoundResource
import com.forcetower.uefs.core.storage.resource.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HourglassRepository @Inject constructor(
    private val database: APIDatabase,
    private val service: APIService,
    private val executors: AppExecutors,
    private val disciplinesRepository: DisciplineDetailsRepository
) {

    fun overview(): LiveData<Resource<List<UDiscipline>>> {
        return object : NetworkBoundResource<List<UDiscipline>, UResponse<UHourOverview>>(executors) {
            override fun loadFromDb() = database.disciplineDao().getAll()
            override fun shouldFetch(it: List<UDiscipline>?) = true
            override fun createCall() = service.overview()
            override fun saveCallResult(value: UResponse<UHourOverview>) {
                val data = value.data
                data ?: return
                database.disciplineDao().insert(data.disciplines)
                database.teacherDao().insert(data.teachers)
            }
        }.asLiveData()
    }

    fun sendData() = disciplinesRepository.contribute()
    fun query(query: String?) = database.disciplineDao().query(query)
}