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
import androidx.room.Transaction
import androidx.room.Update
import com.forcetower.uefs.core.model.unes.ClassMaterial
import com.forcetower.uefs.core.storage.database.aggregation.ClassMaterialWithClass

@Dao
abstract class ClassMaterialDao {
    @Query("SELECT * FROM ClassMaterial WHERE group_id = :classGroupId")
    abstract fun getMaterialsFromGroup(classGroupId: Long): LiveData<List<ClassMaterial>>

    @Query("SELECT * FROM ClassMaterial WHERE class_item_id = :classItemId")
    abstract fun getMaterialsFromClassItem(classItemId: Long): LiveData<List<ClassMaterial>>

    @Query("DELETE FROM ClassMaterial")
    abstract fun deleteAll()

    @WorkerThread
    @Query("DELETE FROM ClassMaterial WHERE group_id = :groupId")
    abstract fun clearFromGroup(groupId: Long)

    @WorkerThread
    @Transaction
    @Query("SELECT cm.* FROM ClassMaterial cm INNER JOIN ClassGroup CG ON cm.group_id = CG.uid INNER JOIN Class C ON CG.class_id = C.uid INNER JOIN Semester S ON C.semester_id = S.uid WHERE S.uid = (SELECT ss.uid FROM Semester ss ORDER BY ss.sagres_id DESC LIMIT 1) AND cm.notified = 0")
    abstract fun getAllUnnotified(): List<ClassMaterialWithClass>

    @WorkerThread
    @Query("UPDATE ClassMaterial SET notified = 1")
    abstract fun markAllNotified()

    @Query("SELECT * FROM ClassMaterial WHERE group_id = :groupId AND name = :name AND link = :link")
    abstract suspend fun getMaterialsByIdentifiers(name: String, link: String, groupId: Long): ClassMaterial?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(material: ClassMaterial): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(material: ClassMaterial)
}
