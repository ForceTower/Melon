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

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.forcetower.uefs.core.model.unes.ClassItem

@Dao
abstract class ClassItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(item: ClassItem): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(item: ClassItem)

    @Query("SELECT * FROM ClassItem WHERE group_id = :groupId AND number = :number")
    abstract suspend fun getItemByIdentifiers(groupId: Long, number: Int): ClassItem?

    @Query("SELECT * FROM ClassItem WHERE group_id = :classGroupId")
    abstract fun getClassItemsFromGroup(classGroupId: Long): LiveData<List<ClassItem>>

    @WorkerThread
    @Query("DELETE FROM ClassItem WHERE group_id = :groupId")
    abstract fun clearFromGroup(groupId: Long)
}
