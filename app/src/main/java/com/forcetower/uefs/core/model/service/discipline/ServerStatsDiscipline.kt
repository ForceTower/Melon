package com.forcetower.uefs.core.model.service.discipline

import com.forcetower.uefs.core.model.service.ClassStatsData
import com.google.gson.annotations.SerializedName

fun List<ClassStatsData>.transformToNewStyle(): List<DisciplineData> {
    // Classes with same identifier means they are a part of a conjunct class (Disciplines separated by T and P)
    return groupBy { it.identifier }.values
            // if the T or the P has no grades, they're stripped
//            .filter { it.isNotEmpty() }
            // iterate over the lists and transform to new style
            .map { list ->
                // The new values are composed by all the grades from all sources of the class
                val values = list.map { SimpleGrade(it.evaluationName, it.evaluationDate, it.evaluationGrade) }
                // The first teacher takes all the grades to himself,
                //
                // since the second (or maybe third) teacher will also be iterated over,
                // they all will receive the grade
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
            }
            // strip data with no grades...
//            .filter { it.values.isNotEmpty() }
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