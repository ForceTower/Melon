package com.forcetower.uefs.core.model.service

import com.forcetower.uefs.core.model.unes.Flowchart
import com.google.gson.annotations.SerializedName
import java.util.Calendar

data class FlowchartDTO(
    val id: Long,
    @SerializedName("course_id")
    val courseId: Long,
    val description: String,
    val semesters: List<FlowchartSemesterDTO>
) {
    fun toFlowchart(): Flowchart {
        return Flowchart(id, courseId, description, Calendar.getInstance().timeInMillis)
    }
}