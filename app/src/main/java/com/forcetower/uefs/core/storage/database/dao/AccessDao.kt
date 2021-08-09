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
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.uefs.core.model.unes.Access

@Dao
abstract class AccessDao {
    @Insert(onConflict = REPLACE)
    abstract fun insert(access: Access)

    @Transaction
    open fun insert(username: String, password: String) {
        val access = findDirect(username, password)
        if (access != null) return
        deleteAll()
        insert(Access(username = username, password = password))
    }

    @Query("SELECT * FROM Access WHERE username = :username AND password = :password LIMIT 1")
    abstract fun findDirect(username: String, password: String): Access?

    @Query("DELETE FROM Access")
    abstract fun deleteAll()

    @Query("SELECT * FROM Access LIMIT 1")
    abstract fun getAccess(): LiveData<Access?>

    @Query("SELECT * FROM Access LIMIT 1")
    abstract fun getAccessDirect(): Access?

    @Query("SELECT * FROM Access LIMIT 1")
    abstract suspend fun getAccessDirectSuspend(): Access?

    @Query("UPDATE Access SET valid = :valid")
    abstract fun setAccessValidation(valid: Boolean)

    @Query("UPDATE Access SET password = :password")
    abstract fun updateAccessPassword(password: String)

    @Query("UPDATE Access SET valid = :valid")
    abstract suspend fun setAccessValidationSuspend(valid: Boolean)

    @Query("UPDATE Access SET password = :password")
    abstract suspend fun updateAccessPasswordSuspend(password: String)

    @Query("SELECT COUNT(uid) FROM Access")
    abstract suspend fun getAccessCount(): Int
}
