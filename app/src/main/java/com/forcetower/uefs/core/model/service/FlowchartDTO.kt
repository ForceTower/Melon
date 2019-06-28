package com.forcetower.uefs.core.model.service

import com.google.gson.annotations.SerializedName

data class FlowchartDTO(
    val id: Long,
    @SerializedName("course_id")
    val courseId: Long,
    val description: String,
    val semesters: List<FlowchartSemesterDTO>
)