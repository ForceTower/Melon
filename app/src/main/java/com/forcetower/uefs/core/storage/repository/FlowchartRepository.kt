package com.forcetower.uefs.core.storage.repository

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.crashlytics.android.Crashlytics
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.api.UResponse
import com.forcetower.uefs.core.model.service.FlowchartDTO
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

    fun getUnifiedRequirementsUI(disciplineId: Long): LiveData<List<FlowchartRequirementUI>> {
        val result = MutableLiveData<List<FlowchartRequirementUI>>()
        executors.diskIO().execute {
            val default = database.flowchartRequirementDao().getDecoratedListDirect(disciplineId)
            val blocked = getRecursiveRequirementsWorker(disciplineId)
            val release = getRecursiveUnlockRequirementWorker(disciplineId)
            val unified = default + blocked + release
            result.postValue(unified)
        }
        return result
    }

    fun getRecursiveRequirementsUI(disciplineId: Long): LiveData<List<FlowchartRequirementUI>> {
        val result = MutableLiveData<List<FlowchartRequirementUI>>()
        executors.others().execute {
            try {
                val everything = getRecursiveRequirementsWorker(disciplineId)
                        .distinctBy { it.requiredDisciplineId }
                        .sortedWith(Comparator { a, b ->
                            val diff = compareValues(a.sequence, b.sequence)
                            if (diff != 0) diff else compareValues(a.shownName, b.shownName)
                        })
                Timber.d("Everything size requirements: ${everything.size}")
                result.postValue(everything)
            } catch (t: Throwable) {
                Timber.e(t, "A stack overflow!")
                Crashlytics.log("Probably a stack overflow happened on requirements!!!")
                Crashlytics.logException(t)
            }
        }
        return result
    }

    fun getRecursiveUnlockRequirementUI(disciplineId: Long): LiveData<List<FlowchartRequirementUI>> {
        val result = MutableLiveData<List<FlowchartRequirementUI>>()
        executors.others().execute {
            try {
                val everything = getRecursiveUnlockRequirementWorker(disciplineId)
                        .distinctBy { it.disciplineId }
                        .sortedWith(Comparator { a, b ->
                            val diff = compareValues(a.sequence, b.sequence)
                            if (diff != 0) diff else compareValues(a.shownName, b.shownName)
                        })
                Timber.d("Everything size unlock: ${everything.size}")
                result.postValue(everything)
            } catch (t: Throwable) {
                Timber.e(t, "A stack overflow!")
                Crashlytics.log("Probably a stack overflow happened on unlock!!!")
                Crashlytics.logException(t)
            }
        }
        return result
    }

    @WorkerThread
    private fun getRecursiveRequirementsWorker(disciplineId: Long?): List<FlowchartRequirementUI> {
        disciplineId ?: return emptyList()

        val list = database.flowchartRequirementDao().getDecoratedListDirect(disciplineId)
        val requirements = list.filter {
            it.requiredDisciplineId != null && it.typeId == 1
        }.map { FlowchartRequirementUI(it.id, recursiveRequirements, it.shownName, it.disciplineId, it.requiredDisciplineId, it.coursePercentage, it.courseHours, -1, it.sequence, it.semesterName, it.completed) }
        Timber.d("Requirements $requirements")
        if (requirements.isEmpty()) return emptyList()

        val iteration = mutableListOf<FlowchartRequirementUI>()
        iteration += requirements

        requirements.forEach {
            val items = getRecursiveRequirementsWorker(it.requiredDisciplineId)
            iteration += items
        }

        return iteration
    }

    @WorkerThread
    private fun getRecursiveUnlockRequirementWorker(disciplineId: Long?): List<FlowchartRequirementUI> {
        disciplineId ?: return emptyList()

        val deps = database.flowchartRequirementDao().getDecoratedDependenciesDirect(disciplineId)
        val dependants = deps.filter {
            it.typeId == 1
        }.map { FlowchartRequirementUI(it.id, recursiveUnlocks, it.shownName, it.disciplineId, it.requiredDisciplineId, it.coursePercentage, it.courseHours, -2, it.sequence, it.semesterName, it.completed) }
        if (dependants.isEmpty()) return emptyList()

        val iteration = mutableListOf<FlowchartRequirementUI>()
        iteration += dependants

        dependants.forEach {
            val items = getRecursiveUnlockRequirementWorker(it.disciplineId)
            iteration += items
        }
        Timber.d("Iteration: $iteration")
        return iteration
    }
}