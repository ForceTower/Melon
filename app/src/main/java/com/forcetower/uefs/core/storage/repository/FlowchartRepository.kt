package com.forcetower.uefs.core.storage.repository

import androidx.lifecycle.LiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.api.UResponse
import com.forcetower.uefs.core.model.service.FlowchartDTO
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
    private val executors: AppExecutors
) {
    fun getFlowchart(courseId: Long): LiveData<Resource<List<FlowchartSemesterUI>>> {
        return object : NetworkBoundResource<List<FlowchartSemesterUI>, UResponse<FlowchartDTO>>(executors) {
            override fun loadFromDb() = database.flowchartSemesterDao().getDecoratedList(courseId)
            override fun shouldFetch(it: List<FlowchartSemesterUI>?) = true
            override fun createCall() = service.getFlowchart(courseId).asLiveData()
            override fun saveCallResult(value: UResponse<FlowchartDTO>) {
                Timber.d("Data received from network $value")
                if (value.data != null) database.flowchartDao().insertFromNetwork(value.data)
            }
        }.asLiveData()
    }

    fun getSemesterUI(semesterId: Long) = database.flowchartSemesterDao().getDecorated(semesterId)
    fun getDisciplinesFromSemester(semesterId: Long) = database.flowchartDisciplineDao().getDecoratedList(semesterId)
    fun getDisciplineUI(disciplineId: Long) = database.flowchartDisciplineDao().getDecorated(disciplineId)
    fun getSemesterName(disciplineId: Long) = database.flowchartDisciplineDao().getSemesterFromDiscipline(disciplineId)
    fun getRequirementsUI(disciplineId: Long) = database.flowchartRequirementDao().getDecoratedList(disciplineId)
}