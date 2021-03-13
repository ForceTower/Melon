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
import androidx.room.ForeignKey.SET_NULL
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forcetower.sagres.database.model.SagresDisciplineGroup
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(entity = Class::class, parentColumns = ["uid"], childColumns = ["class_id"], onDelete = CASCADE, onUpdate = CASCADE),
        ForeignKey(entity = Teacher::class, parentColumns = ["uid"], childColumns = ["teacher_id"], onDelete = SET_NULL, onUpdate = CASCADE)
    ],
    indices = [
        Index(value = ["class_id", "group"], unique = true),
        Index(value = ["uuid"], unique = true),
        Index(value = ["teacher_id"], unique = false)
    ]
)
data class ClassGroup(
    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0,
    @ColumnInfo(name = "class_id")
    val classId: Long,
    var group: String,
    var teacher: String? = null,
    var credits: Int = 0,
    val uuid: String = UUID.randomUUID().toString(),
    var draft: Boolean = true,
    var ignored: Boolean = false,
    @ColumnInfo(name = "teacher_id")
    var teacherId: Long? = null,
    val sagresId: Long? = null,
    val teacherEmail: String? = null
) {

    fun selectiveCopy(grp: SagresDisciplineGroup) {
        if (!grp.group.isNullOrBlank()) group = grp.group!!
        if (!grp.teacher.isNullOrBlank()) teacher = grp.teacher
        if (grp.credits > 0) credits = grp.credits
        if (draft) draft = grp.isDraft
    }

    override fun toString(): String {
        return "${classId}_$group draft: $draft"
    }
}
