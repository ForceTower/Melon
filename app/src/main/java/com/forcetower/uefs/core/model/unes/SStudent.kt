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
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    indices = [
        Index(value = ["name"])
    ]
)
data class SStudent(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("student_id")
    val id: Long,
    @SerializedName("user_id")
    val userId: Long,
    val name: String,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("course_id")
    val course: Int?,
    @SerializedName("course_name")
    val courseName: String?,
    val me: Boolean = false
)

data class SStudentDTO(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("student_id")
    val id: Long,
    @SerializedName("user_id")
    val userId: Long,
    val name: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("course_id")
    val course: Int?,
    @SerializedName("course_name")
    val courseName: String?,
    val me: Boolean?,
    val statements: List<ProfileStatement>? = null
) {
    fun toCommon() = SStudent(id, userId, name ?: "Desconhecido", imageUrl, course, courseName, me ?: false)
}
