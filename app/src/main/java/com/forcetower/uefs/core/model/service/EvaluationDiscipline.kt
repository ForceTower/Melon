package com.forcetower.uefs.core.model.service

import com.google.gson.annotations.SerializedName

data class EvaluationDiscipline(
    @SerializedName("discipline_id")
    val disciplineId: Long,
    val department: String,
    val code: String,
    val name: String,
    val mean: Double,
    @SerializedName("qtd_students")
    val qtdStudents: Int,
    val participant: Boolean = false
)
