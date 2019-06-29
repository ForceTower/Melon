package com.forcetower.uefs.feature.flowchart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.unes.FlowchartDisciplineUI
import com.forcetower.uefs.core.model.unes.FlowchartSemesterUI
import com.forcetower.uefs.core.storage.repository.FlowchartRepository
import com.forcetower.uefs.core.vm.Event
import com.forcetower.uefs.feature.flowchart.home.SemesterInteractor
import com.forcetower.uefs.feature.flowchart.semester.DisciplineInteractor
import javax.inject.Inject

class FlowchartViewModel @Inject constructor(
    private val repository: FlowchartRepository
) : ViewModel(), SemesterInteractor, DisciplineInteractor {
    private val _onSemesterSelect = MutableLiveData<Event<FlowchartSemesterUI>>()
    val onSemesterSelect: LiveData<Event<FlowchartSemesterUI>>
        get() = _onSemesterSelect

    private val _onDisciplineSelect = MutableLiveData<Event<FlowchartDisciplineUI>>()
    val onDisciplineSelect: LiveData<Event<FlowchartDisciplineUI>>
        get() = _onDisciplineSelect

    override fun onSemesterSelected(semester: FlowchartSemesterUI) {
        _onSemesterSelect.value = Event(semester)
    }

    override fun onDisciplineSelected(discipline: FlowchartDisciplineUI) {
        _onDisciplineSelect.value = Event(discipline)
    }
}