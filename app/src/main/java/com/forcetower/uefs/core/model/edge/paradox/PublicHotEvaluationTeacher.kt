package com.forcetower.uefs.core.model.edge.paradox

import com.google.gson.annotations.SerializedName

data class PublicHotEvaluationTeacher(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("mean")
    val mean: Double,
    @SerializedName("studentCount")
    val studentCount: Int
)