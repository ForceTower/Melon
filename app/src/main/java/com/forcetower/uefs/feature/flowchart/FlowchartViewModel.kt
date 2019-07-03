package com.forcetower.uefs.feature.flowchart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.unes.FlowchartDisciplineUI
import com.forcetower.uefs.core.model.unes.FlowchartRequirementUI
import com.forcetower.uefs.core.model.unes.FlowchartSemesterUI
import com.forcetower.uefs.core.storage.repository.FlowchartRepository
import com.forcetower.uefs.core.vm.Event
import com.forcetower.uefs.feature.flowchart.home.SemesterInteractor
import com.forcetower.uefs.feature.flowchart.semester.DisciplineInteractor
import com.forcetower.uefs.feature.shared.extensions.setValueIfNew
import javax.inject.Inject

class FlowchartViewModel @Inject constructor(
    private val repository: FlowchartRepository
) : ViewModel(), SemesterInteractor, DisciplineInteractor {
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

    fun getDisciplinesUI(semesterId: Long) = repository.getDisciplinesFromSemester(semesterId)
    fun getDisciplineUi(disciplineId: Long) = repository.getDisciplineUI(disciplineId)
    fun getSemesterName(disciplineId: Long) = repository.getSemesterName(disciplineId)
    fun getSemesterUI(semesterId: Long) = repository.getSemesterUI(semesterId)
    fun getRequirementsUI(disciplineId: Long) = repository.getUnifiedRequirementsUI(disciplineId)
    fun getRecursiveRequirementsUI(disciplineId: Long) = repository.getRecursiveRequirementsUI(disciplineId)
    fun getRecursiveUnlockRequirementUI(disciplineId: Long) = repository.getRecursiveUnlockRequirementUI(disciplineId)

    fun setCourse(courseId: Long) {
        _courseId.setValueIfNew(courseId)
    }
}