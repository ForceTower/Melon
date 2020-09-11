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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(entity = AffinityQuestion::class, childColumns = ["question_id"], parentColumns = ["id"], onDelete = CASCADE, onUpdate = CASCADE),
        ForeignKey(entity = SStudent::class, childColumns = ["student_id"], parentColumns = ["id"], onDelete = CASCADE, onUpdate = CASCADE)
    ],
    indices = [
        Index("student_id", unique = false),
        Index("question_id", unique = false)
    ]
)
data class AffinityQuestionAlternative(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    @ColumnInfo(name = "question_id")
    val questionId: Long,
    @ColumnInfo(name = "student_id")
    val studentId: Long
)
