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

package com.forcetower.uefs.feature.flowchart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.core.model.unes.Flowchart
import com.forcetower.uefs.core.model.unes.FlowchartDisciplineUI
import com.forcetower.uefs.core.model.unes.FlowchartRequirementUI
import com.forcetower.uefs.core.model.unes.FlowchartSemesterUI
import com.forcetower.uefs.core.storage.repository.FlowchartRepository
import com.forcetower.uefs.feature.flowchart.home.SemesterInteractor
import com.forcetower.uefs.feature.flowchart.semester.DisciplineInteractor
import com.forcetower.uefs.feature.shared.extensions.setValueIfNew
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FlowchartViewModel @Inject constructor(
    private val repository: FlowchartRepository
) : ViewModel(), FlowchartInteractor, SemesterInteractor, DisciplineInteractor {
    private val _courseId = MutableLiveData<Long>()

    private val _onSemesterSelect = MutableLiveData<Event<FlowchartSemesterUI>>()
    val onSemesterSelect: LiveData<Event<FlowchartSemesterUI>>
        get() = _onSemesterSelect

    private val _flowchart = MediatorLiveData<List<FlowchartSemesterUI>>()
    val flowchart: LiveData<List<FlowchartSemesterUI>>
        get() = _flowchart

    private val _onDisciplineSelect = MutableLiveData<Event<FlowchartDisciplineUI>>()
    val onDisciplineSelect: LiveData<Event<FlowchartDisciplineUI>>
        get() = _onDisciplineSelect

    private val _onRequirementSelect = MutableLiveData<Event<FlowchartRequirementUI>>()
    val onRequirementSelect: LiveData<Event<FlowchartRequirementUI>>
        get() = _onRequirementSelect

    private val _onFlowchartSelect = MutableLiveData<Event<Flowchart>>()
    val onFlowchartSelect: LiveData<Event<Flowchart>>
        get() = _onFlowchartSelect

    init {
        _flowchart.addSource(_courseId) { courseId ->
            val source = repository.getFlowchart(courseId)
            _flowchart.addSource(source) { value ->
                if (value.data != null) {
                    _flowchart.value = value.data
                }
            }
        }
    }

    override fun onSemesterSelected(semester: FlowchartSemesterUI) {
        _onSemesterSelect.value = Event(semester)
    }

    override fun onDisciplineSelected(discipline: FlowchartDisciplineUI) {
        _onDisciplineSelect.value = Event(discipline)
    }

    override fun onRequirementSelected(requirementUI: FlowchartRequirementUI) {
        _onRequirementSelect.value = Event(requirementUI)
    }

    override fun onFlowchartSelected(flow: Flowchart) {
        _onFlowchartSelect.value = Event(flow)
    }

    fun getFlowcharts() = repository.getFlowcharts()
    fun getDisciplinesUI(semesterId: Long) = repository.getDisciplinesFromSemester(semesterId)
    fun getDisciplineUi(disciplineId: Long) = repository.getDisciplineUI(disciplineId)
    fun getSemesterName(disciplineId: Long) = repository.getSemesterName(disciplineId)
    fun getSemesterUI(semesterId: Long) = repository.getSemesterUI(semesterId)
    fun getRequirementsUI(disciplineId: Long) = repository.getUnifiedRequirementsUI(disciplineId)

    fun setCourse(courseId: Long) {
        _courseId.setValueIfNew(courseId)
    }
}
