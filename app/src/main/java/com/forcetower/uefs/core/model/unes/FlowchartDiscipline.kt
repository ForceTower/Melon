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

@Entity(
    foreignKeys = [
        ForeignKey(entity = Discipline::class, childColumns = ["disciplineId"], parentColumns = ["uid"], onUpdate = CASCADE, onDelete = CASCADE),
        ForeignKey(entity = FlowchartSemester::class, childColumns = ["semesterId"], parentColumns = ["id"], onUpdate = CASCADE, onDelete = CASCADE)
    ],
    indices = [
        Index(value = ["disciplineId"]),
        Index(value = ["semesterId"])
    ]
)
data class FlowchartDiscipline(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val disciplineId: Long,
    val type: String,
    val mandatory: Boolean,
    val semesterId: Long,
    val completed: Boolean,
    val participating: Boolean
)

data class FlowchartDisciplineUI(
    val id: Long,
    val type: String,
    val mandatory: Boolean,
    val name: String,
    val code: String,
    val credits: Int,
    val department: String,
    val program: String? = null,
    val completed: Boolean,
    val participating: Boolean
)
