package com.forcetower.uefs.core.model.edge.paradox

import com.google.gson.annotations.SerializedName

data class PublicDisciplineEvaluationCombinedData(
    @SerializedName("disciplineId")
    val disciplineId: String,
    @SerializedName("discipline")
    val discipline: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("departmentName")
    val departmentName: String,
    @SerializedName("mean")
    val mean: Double,
    @SerializedName("studentCount")
    val studentCount: Int,
    @SerializedName("studentCountWeighted")
    val studentCountWeighted: Int,
    @SerializedName("participating")
    val participating: Boolean,
    @SerializedName("approved")
    val approved: Int,
    @SerializedName("failed")
    val failed: Int,
    @SerializedName("quit")
    val quit: Int,
    @SerializedName("teachers")
    val teachers: List<PublicDisciplineEvaluationData>
)

data class PublicDisciplineEvaluationData(
    @SerializedName("semester")
    val semester: String,
    @SerializedName("semesterStart")
    val semesterStart: String,
    @SerializedName("semesterPlatformId")
    val semesterPlatformId: Long,
    @SerializedName("teacherId")
    val teacherId: String,
    @SerializedName("teacherName")
    val teacherName: String,
    @SerializedName("mean")
    val mean: Double,
    @SerializedName("studentCountWeighted")
    val studentCountWeighted: Int,
    @SerializedName("studentsCount")
    val studentsCount: Int,
    @SerializedName("approved")
    val approved: Int,
    @SerializedName("failed")
    val failed: Int,
    @SerializedName("quit")
    val quit: Int
)
