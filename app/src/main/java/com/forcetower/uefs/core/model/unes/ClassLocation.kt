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
        ForeignKey(entity = ClassGroup::class, parentColumns = ["uid"], childColumns = ["group_id"], onUpdate = CASCADE, onDelete = CASCADE),
        ForeignKey(entity = Profile::class, parentColumns = ["uid"], childColumns = ["profile_id"], onUpdate = CASCADE, onDelete = CASCADE)
    ],
    indices = [
        Index(value = ["group_id", "day", "starts_at", "ends_at", "profile_id"], unique = true),
        Index(value = ["profile_id"]),
        Index(value = ["uuid"], unique = true)
    ]
)
data class ClassLocation(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    @ColumnInfo(name = "group_id")
    val groupId: Long,
    @ColumnInfo(name = "profile_id")
    val profileId: Long,
    @ColumnInfo(name = "starts_at")
    val startsAt: String,
    @ColumnInfo(name = "ends_at")
    val endsAt: String,
    val day: String,
    val room: String?,
    val modulo: String?,
    val campus: String?,
    val uuid: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "hidden_on_schedule")
    val hiddenOnSchedule: Boolean = false,
    val startsAtInt: Int,
    val endsAtInt: Int,
    val dayInt: Int
) : Comparable<ClassLocation> {

    override fun toString(): String {
        return "$groupId: $day >> $startsAt .. $endsAt (hidden: $hiddenOnSchedule)"
    }

    override fun compareTo(other: ClassLocation): Int {
        return startsAt.compareTo(other.startsAt)
    }
}
