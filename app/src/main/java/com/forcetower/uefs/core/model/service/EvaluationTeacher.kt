package com.forcetower.uefs.core.model.service

import com.google.gson.annotations.SerializedName

data class EvaluationTeacher(
    @SerializedName("teacher_id")
    val teacherId: Long,
    val name: String,
    val mean: Double,
    @SerializedName("qtd_students")
    val qtdStudents: Int
)
