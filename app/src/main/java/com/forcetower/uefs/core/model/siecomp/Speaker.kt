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

package com.forcetower.uefs.core.model.siecomp

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    indices = [
        Index(value = ["uuid"], unique = true)
    ]
)
data class Speaker(
    @SerializedName(value = "uid", alternate = ["id"])
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val name: String = "",
    var image: String? = null,
    val lab: String? = "",
    @SerializedName(value = "abstract")
    val resume: String? = "",
    val url: String?,
    val github: String?,
    val uuid: String = ""
) {
    fun hasLab() = !lab.isNullOrBlank() && lab != "null"
    fun hasAbstract() = !resume.isNullOrBlank()

    override fun toString(): String {
        return name
    }
}
