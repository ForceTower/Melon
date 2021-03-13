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

package com.forcetower.uefs.core.model.service

import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName

data class ClassStatsData(
    val code: String,
    @ColumnInfo(name = "discipline")
    @SerializedName("discipline_name")
    val disciplineName: String,
    @SerializedName("credits")
    @ColumnInfo(name = "credits")
    val disciplineCredits: Int,
    val semester: Int,
    @ColumnInfo(name = "semester_name")
    @SerializedName("semester_name")
    val semesterName: String,
    val teacher: String,
    val teacherEmail: String?,
    val grade: Double?,
    @SerializedName("partial_score")
    val partialScore: Double?,
    val group: String,
    val identifier: Int,
    @ColumnInfo(name = "eval_grade")
    val evaluationGrade: String?,
    @ColumnInfo(name = "eval_name")
    val evaluationName: String?,
    @ColumnInfo(name = "eval_date")
    val evaluationDate: String?
) {
    companion object {
        const val STATS_CONTRIBUTION = "stats_contribution"
    }
}
