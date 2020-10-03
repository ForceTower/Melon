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
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(entity = Class::class, parentColumns = ["uid"], childColumns = ["class_id"], onUpdate = CASCADE, onDelete = CASCADE),
        ForeignKey(entity = Profile::class, parentColumns = ["uid"], childColumns = ["profile_id"], onUpdate = CASCADE, onDelete = CASCADE)
    ],
    indices = [
        Index(value = ["profile_id"]),
        Index(value = ["class_id"], unique = false),
        Index(value = ["class_id", "profile_id", "sequence", "grouping"], unique = true),
        Index(value = ["uuid"], unique = true)
    ]
)
data class ClassAbsence(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    @ColumnInfo(name = "class_id")
    val classId: Long,
    @ColumnInfo(name = "profile_id")
    val profileId: Long,
    val sequence: Int,
    val description: String,
    val date: String,
    val grouping: String,
    val uuid: String = UUID.randomUUID().toString(),
    val notified: Boolean
) {
    override fun toString(): String {
        return "[sequence: $sequence]"
    }

    fun isSame(other: ClassAbsence) =
        other.classId == classId &&
            other.profileId == profileId &&
            other.sequence == sequence &&
            other.grouping == grouping
}
