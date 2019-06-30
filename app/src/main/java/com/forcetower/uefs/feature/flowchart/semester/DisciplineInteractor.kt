package com.forcetower.uefs.feature.flowchart.semester

import com.forcetower.uefs.core.model.unes.FlowchartDisciplineUI
import com.forcetower.uefs.core.model.unes.FlowchartRequirementUI

interface DisciplineInteractor {
    fun onDisciplineSelected(discipline: FlowchartDisciplineUI)
    fun onRequirementSelected(requirementUI: FlowchartRequirementUI)
}
