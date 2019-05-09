package com.forcetower.uefs.core.model.service.discipline

import com.forcetower.uefs.core.model.service.ClassStatsData
import com.google.gson.annotations.SerializedName

fun List<ClassStatsData>.transformToNewStyle(): List<DisciplineData> {
    return groupBy { it.identifier }.values
            .filter { it.isNotEmpty() }
            .map { list ->
        val values = list.map { SimpleGrade(it.evaluationName, it.evaluationDate, it.evaluationGrade) }
        val first = list[0]
        DisciplineData(
                first.code,
                first.disciplineName,
                first.disciplineCredits,
                first.group,
                first.semester,
                first.semesterName,
                first.teacher,
                first.grade,
                first.partialScore,
                values
        )
    }.filter { it.values.isNotEmpty() }
}

data class DisciplineDetailsData(
    val semester: Int,
    val score: Double,
    val course: Long?,
    val stats: List<DisciplineData>
)

data class DisciplineData(
    @SerializedName("code")
    val disciplineCode: String,
    @SerializedName("discipline_name")
    val disciplineName: String,
    @SerializedName("credits")
    val disciplineCredits: Int,
    @SerializedName("group")
    val disciplineGroup: String,
    @SerializedName("semester")
    val sagresSemesterId: Int,
    @SerializedName("semester_name")
    val semesterName: String,
    @SerializedName("teacher")
    val teacherName: String,
    @SerializedName("grade")
    val finalGrade: Double?,
    @SerializedName("partial_score")
    val partialGrade: Double?,
    @SerializedName("info")
    val values: List<SimpleGrade>
)

data class SimpleGrade(
    val name: String,
    val date: String? = null,
    val value: String? = null
)