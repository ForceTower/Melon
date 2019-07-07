package com.forcetower.uefs.feature.flowchart

import com.forcetower.uefs.core.model.unes.Flowchart

interface FlowchartInteractor {
    fun onFlowchartSelected(flow: Flowchart)
}