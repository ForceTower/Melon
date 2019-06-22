package com.forcetower.uefs.core.model.service

import com.google.gson.annotations.SerializedName

data class EvaluationDiscipline(
    @SerializedName("discipline_id")
    val disciplineId: Long,
    val department: String,
    @SerializedName("department_name")
    val departmentName: String?,
    val code: String,
    val name: String,
    val mean: Double?,
    @SerializedName("qtd_students")
    val qtdStudents: Int = 0,
    val participant: Boolean = false,
    val teachers: List<EvaluationDisciplineTeacher>? = null
)

data class EvaluationDisciplineTeacher(
    @SerializedName("teacher_id")
    val teacherId: Long,
    val name: String,
    val mean: Double,
    @SerializedName("qtd_students")
    val qtdStudents: Int,
    val semester: String,
    @SerializedName("semester_system_id")
    val semesterSystemId: Long
)
