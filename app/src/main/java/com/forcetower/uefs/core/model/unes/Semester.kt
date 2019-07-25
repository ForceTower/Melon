/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forcetower.sagres.database.model.SSemester

@Entity(indices = [Index(value = ["sagres_id"], unique = true)])
data class Semester(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    @ColumnInfo(name = "sagres_id")
    val sagresId: Long,
    val name: String,
    val codename: String,
    val start: Long? = null,
    val end: Long? = null,
    @ColumnInfo(name = "start_class")
    val startClass: Long? = null,
    @ColumnInfo(name = "end_class")
    val endClass: Long? = null
) {
    companion object {
        fun fromSagres(s: SSemester) =
                Semester(0, s.uefsId, s.name.trim(), s.codename.trim(), s.startInMillis, s.endInMillis, s.startClassesInMillis, s.endClassesInMillis)
    }
}