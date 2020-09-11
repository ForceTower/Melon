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

package com.forcetower.uefs.core.storage.apidatabase.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.forcetower.uefs.core.model.api.UTeacher
import java.util.Locale

@Dao
abstract class TeacherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(teachers: List<UTeacher>)

    open fun query(query: String?): LiveData<List<UTeacher>> {
        return if (query.isNullOrBlank()) {
            getAll()
        } else {
            val string = "%${query.toUpperCase(Locale.getDefault())}%"
            doQuery(string)
        }
    }

    // TODO This should be changed to a FTS table for fast scans
    @Query("SELECT * FROM UTeacher WHERE UPPER(name) LIKE :query ORDER BY name")
    protected abstract fun doQuery(query: String): LiveData<List<UTeacher>>

    @Query("SELECT * FROM UTeacher ORDER BY name")
    abstract fun getAll(): LiveData<List<UTeacher>>
}
