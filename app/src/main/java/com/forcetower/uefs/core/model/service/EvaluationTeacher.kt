package com.forcetower.uefs.core.model.service

import com.google.gson.annotations.SerializedName

data class EvaluationTeacher(
    @SerializedName("teacher_id")
    val teacherId: Long,
    val name: String,
    val mean: Double,
    @SerializedName("qtd_students")
    val qtdStudents: Int,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("last_seen")
    val lastSeen: String?,
    @SerializedName("first_seen")
    val firstSeen: String?,
    val approved: Int?,
    val failed: Int?,
    val finals: Int?,
    val disciplines: List<EvaluationDiscipline>?,
    val email: String?
)
