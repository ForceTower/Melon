package com.forcetower.uefs.core.model.service

import com.google.gson.annotations.SerializedName

data class EvaluationHomeTopic(
    val id: Int,
    val title: String,
    val description: String,
    @SerializedName("data_type")
    val dataType: Int = 0,
    val teachers: List<EvaluationTeacher>?,
    val disciplines: List<EvaluationDiscipline>?
)
