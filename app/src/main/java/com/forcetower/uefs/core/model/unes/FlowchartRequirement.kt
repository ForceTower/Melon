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

package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    foreignKeys = [
        ForeignKey(entity = FlowchartDiscipline::class, childColumns = ["disciplineId"], parentColumns = ["id"], onDelete = CASCADE, onUpdate = CASCADE),
        ForeignKey(entity = FlowchartDiscipline::class, childColumns = ["requiredDisciplineId"], parentColumns = ["id"], onDelete = CASCADE, onUpdate = CASCADE)
    ],
    indices = [
        Index(value = ["disciplineId"]),
        Index(value = ["requiredDisciplineId"])
    ]
)
data class FlowchartRequirement(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val type: String,
    @SerializedName("discipline_id")
    val disciplineId: Long,
    @SerializedName("required_discipline_id")
    val requiredDisciplineId: Long?,
    @SerializedName("course_percentage")
    val coursePercentage: Double?,
    @SerializedName("course_hours")
    val courseHours: Long?,
    @SerializedName("type_id")
    val typeId: Int
)

data class FlowchartRequirementUI(
    val id: Long,
    val type: String,
    val shownName: String?,
    val disciplineId: Long,
    val requiredDisciplineId: Long?,
    val coursePercentage: Long?,
    val courseHours: Long?,
    val typeId: Int,
    val sequence: Int?,
    val semesterName: String?,
    val completed: Boolean
)
