package com.forcetower.uefs.core.model.edge.paradox

import com.google.gson.annotations.SerializedName

data class EvaluationHotTopic(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("disciplines")
    val disciplines: List<PublicHotEvaluationDiscipline>? = null,
    @SerializedName("teachers")
    val teachers: List<PublicHotEvaluationTeacher>? = null
)
