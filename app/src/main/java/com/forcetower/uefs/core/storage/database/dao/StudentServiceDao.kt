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

package com.forcetower.uefs.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.forcetower.uefs.core.model.unes.SStudent

@Dao
interface StudentServiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(values: List<SStudent>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSingle(value: SStudent)

    @Query("SELECT * FROM SStudent WHERE id = :profileId")
    fun getProfileById(profileId: Long): LiveData<SStudent>

    @Query("SELECT * FROM SStudent WHERE me = 1")
    fun getMeStudent(): LiveData<SStudent>

    @Query("UPDATE SStudent SET me = 0")
    suspend fun markNoOneAsMe()
}
