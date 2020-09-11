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
import com.forcetower.uefs.core.model.api.UDiscipline
import java.util.Locale

@Dao
abstract class DisciplineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(disciplines: List<UDiscipline>)

    open fun query(query: String?): LiveData<List<UDiscipline>> {
        return if (query.isNullOrBlank()) {
            getAll()
        } else {
            val string = "%${query.toUpperCase(Locale.getDefault())}%"
            doQuery(string)
        }
    }

    // TODO This should be changed to a FTS table for fast scans
    @Query("SELECT * FROM UDiscipline WHERE UPPER(code) LIKE :query OR UPPER(name) LIKE :query ORDER BY name")
    protected abstract fun doQuery(query: String): LiveData<List<UDiscipline>>

    @Query("SELECT * FROM UDiscipline ORDER BY name")
    abstract fun getAll(): LiveData<List<UDiscipline>>
}
