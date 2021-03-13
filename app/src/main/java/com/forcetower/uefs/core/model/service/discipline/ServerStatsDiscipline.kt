/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
            val values = list.map { SimpleGrade(it.evaluationName, it.evaluationDate, it.evaluationGrade) }.filter { it.name != null }
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
                first.teacherEmail,
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
    @SerializedName("teacherEmail")
    val teacherEmail: String?,
    @SerializedName("grade")
    val finalGrade: Double?,
    @SerializedName("partial_score")
    val partialGrade: Double?,
    @SerializedName("info")
    val values: List<SimpleGrade>
)

data class SimpleGrade(
    val name: String? = null,
    val date: String? = null,
    val value: String? = null
)
