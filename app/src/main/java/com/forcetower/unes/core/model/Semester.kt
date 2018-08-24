/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.unes.core.model

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