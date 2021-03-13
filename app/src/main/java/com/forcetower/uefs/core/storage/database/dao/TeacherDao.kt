/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.forcetower.uefs.core.model.unes.Teacher

@Dao
abstract class TeacherDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIgnore(value: Teacher)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(value: Teacher): Long

    @Update
    abstract suspend fun update(value: Teacher)

    /**
     * This method uses the internal portal systemId to uniquely identify teachers.
     * If the systemId stops working, it will use the name hash as system id which is not good :D
     */
    @Transaction
    open suspend fun insertOrUpdate(value: Teacher): Long {
        val systemId = value.sagresId ?: value.name.hashCode().toLong()
        val existing = findBySystemId(systemId)
        return if (existing != null) {
            val copy = value.copy(uid = existing.uid)
            update(copy)
            existing.uid
        } else {
            return insert(value)
        }
    }

    @Query("SELECT * FROM Teacher WHERE sagresId = :systemId")
    abstract suspend fun findBySystemId(systemId: Long): Teacher?
}
