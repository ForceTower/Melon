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

/**
 * Notified Status
 * 0 -> No Changes
 * 1 -> Created with no grade
 * 2 -> Date changed
 * 3 -> Grade posted
 * 4 -> Grade changed
 */
@Entity(
    foreignKeys = [
        ForeignKey(entity = Class::class, parentColumns = ["uid"], childColumns = ["class_id"], onUpdate = CASCADE, onDelete = CASCADE)
    ],
    indices = [
        Index(value = ["class_id"]),
        Index(value = ["name", "class_id", "grouping"], unique = true)
    ]
)
data class Grade(
    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0,
    @ColumnInfo(name = "class_id")
    val classId: Long,
    val name: String,
    var date: String?,
    var grade: String?,
    var grouping: Int,
    var groupingName: String,
    var notified: Int = 0
) {
    fun hasGrade(): Boolean {
        val grade = this.grade
        return (
            grade != null &&
                grade.trim().isNotEmpty() &&
                !grade.trim().equals("Não Divulgada", ignoreCase = true) &&
                !grade.trim().equals("-", ignoreCase = true) &&
                !grade.trim().equals("--", ignoreCase = true) &&
                !grade.trim().equals("*", ignoreCase = true) &&
                !grade.trim().equals("**", ignoreCase = true) &&
                !grade.trim().equals("-1", ignoreCase = true)
            )
    }

    fun gradeDouble() = grade?.trim()
        ?.replace(",", ".")
        ?.replace("-", "")
        ?.replace("*", "")
        ?.toDoubleOrNull()

    override fun toString(): String = "${name}_${grade}_${date}_$notified"
}
