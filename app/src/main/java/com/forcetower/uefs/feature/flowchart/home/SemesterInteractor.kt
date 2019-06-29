package com.forcetower.uefs.feature.flowchart.home

import com.forcetower.uefs.core.model.unes.FlowchartSemesterUI

interface SemesterInteractor {
    fun onSemesterSelected(semester: FlowchartSemesterUI)
}
