package com.forcetower.uefs.core.model.edge.paradox

import com.google.gson.annotations.SerializedName

data class PublicHotEvaluationDiscipline(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("departmentName")
    val departmentName: String,
    @SerializedName("departmentCode")
    val departmentCode: String,
    @SerializedName("mean")
    val mean: Double,
    @SerializedName("studentCount")
    val studentCount: Int
)