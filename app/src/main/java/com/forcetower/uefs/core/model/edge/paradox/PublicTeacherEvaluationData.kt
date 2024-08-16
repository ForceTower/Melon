package com.forcetower.uefs.core.model.edge.paradox

import com.google.gson.annotations.SerializedName

data class PublicTeacherEvaluationData(
    @SerializedName("disciplineId")
    val disciplineId: String,
    @SerializedName("departmentCode")
    val departmentCode: String,
    @SerializedName("departmentName")
    val departmentName: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("mean")
    val mean: Double,
    @SerializedName("studentCountWeighted")
    val studentCountWeighted: Int,
    @SerializedName("studentCount")
    val studentCount: Int
)

data class PublicTeacherEvaluationCombinedData(
    @SerializedName("teacherId")
    val teacherId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("mean")
    val mean: Double,
    @SerializedName("studentCountWeighted")
    val studentCountWeighted: Int,
    @SerializedName("studentCount")
    val studentCount: Int,
    @SerializedName("approved")
    val approved: Int,
    @SerializedName("failed")
    val failed: Int,
    @SerializedName("quit")
    val quit: Int,
    @SerializedName("firstSeen")
    val firstSeen: String,
    @SerializedName("lastSeen")
    val lastSeen: String,
    @SerializedName("participant")
    val participant: Boolean,
    @SerializedName("disciplines")
    val disciplines: List<PublicTeacherEvaluationData>
)