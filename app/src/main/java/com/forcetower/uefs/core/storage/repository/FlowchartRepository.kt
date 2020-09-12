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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.api.UResponse
import com.forcetower.uefs.core.model.service.FlowchartDTO
import com.forcetower.uefs.core.model.unes.Flowchart
import com.forcetower.uefs.core.model.unes.FlowchartRequirementUI
import com.forcetower.uefs.core.model.unes.FlowchartSemesterUI
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.network.adapter.asLiveData
import com.forcetower.uefs.core.storage.resource.NetworkBoundResource
import com.forcetower.uefs.core.storage.resource.Resource
import timber.log.Timber
import javax.inject.Inject

class FlowchartRepository @Inject constructor(
    private val database: UDatabase,
    private val service: UService,
    private val executors: AppExecutors,
    context: Context
) {
    private val recursiveRequirements = context.getString(R.string.flowchart_recursive_continuation)
    private val recursiveUnlocks = context.getString(R.string.flowchart_recursive_unlock)

    fun getFlowcharts(): LiveData<Resource<List<Flowchart>>> {
        return object : NetworkBoundResource<List<Flowchart>, UResponse<List<Flowchart>>>(executors) {
            override fun loadFromDb() = database.flowchartDao().getFlowcharts()
            override fun shouldFetch(it: List<Flowchart>?) = true
            override fun createCall() = service.getFlowcharts().asLiveData()
            override fun saveCallResult(value: UResponse<List<Flowchart>>) {
                Timber.d("Result is ${value.data}")
                value.data ?: return
                database.flowchartDao().insertFlowcharts(value.data)
            }
        }.asLiveData()
    }

    fun getFlowchart(courseId: Long): LiveData<Resource<List<FlowchartSemesterUI>>> {
        return object : NetworkBoundResource<List<FlowchartSemesterUI>, UResponse<FlowchartDTO>>(executors) {
            override fun loadFromDb() = database.flowchartSemesterDao().getDecoratedList(courseId)
            override fun shouldFetch(it: List<FlowchartSemesterUI>?) = true
            override fun createCall() = service.getFlowchart(courseId).asLiveData()
            override fun saveCallResult(value: UResponse<FlowchartDTO>) {
                if (value.data != null) database.flowchartDao().insertFromNetwork(value.data)
            }
        }.asLiveData()
    }

    fun getSemesterUI(semesterId: Long) = database.flowchartSemesterDao().getDecorated(semesterId)
    fun getDisciplinesFromSemester(semesterId: Long) = database.flowchartDisciplineDao().getDecoratedList(semesterId)
    fun getDisciplineUI(disciplineId: Long) = database.flowchartDisciplineDao().getDecorated(disciplineId)
    fun getSemesterName(disciplineId: Long) = database.flowchartDisciplineDao().getSemesterFromDiscipline(disciplineId)
    fun getUnifiedRequirementsUI(disciplineId: Long): LiveData<List<FlowchartRequirementUI>> {
        val result = MutableLiveData<List<FlowchartRequirementUI>>()
        executors.diskIO().execute {
            val default = database.flowchartRequirementDao().getDecoratedListCommonDirect(disciplineId)
            val blocked = database.flowchartRequirementDao().getRecursiveRequirementsUI(disciplineId, recursiveRequirements)
            val release = database.flowchartRequirementDao().getRecursiveUnlockRequirementUI(disciplineId, recursiveUnlocks)
            val unified = default + blocked + release
            result.postValue(unified)
        }
        return result
    }
}
