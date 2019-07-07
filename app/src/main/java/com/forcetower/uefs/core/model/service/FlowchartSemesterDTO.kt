package com.forcetower.uefs.core.model.service

import com.forcetower.uefs.core.model.unes.FlowchartSemester
import com.google.gson.annotations.SerializedName

data class FlowchartSemesterDTO(
    val id: Long,
    @SerializedName("flowchart_id")
    val flowchartId: Long,
    val order: Int,
    val name: String,
    val disciplines: List<FlowchartDisciplineDTO>
) {
    fun toSemester(): FlowchartSemester {
        return FlowchartSemester(id, flowchartId, order, name)
    }
}
