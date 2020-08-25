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

import com.google.gson.annotations.SerializedName

data class EvaluationTeacher(
    @SerializedName("teacher_id")
    val teacherId: Long,
    val name: String,
    val mean: Double?,
    @SerializedName("qtd_students")
    val qtdStudents: Int?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("last_seen")
    val lastSeen: String?,
    @SerializedName("first_seen")
    val firstSeen: String?,
    val approved: Int?,
    val failed: Int?,
    val finals: Int?,
    val email: String?,
    val disciplines: List<EvaluationDiscipline>? = null,
    val participant: Boolean? = false
)
