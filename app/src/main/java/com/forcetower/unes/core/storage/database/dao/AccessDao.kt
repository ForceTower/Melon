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

package com.forcetower.unes.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.unes.core.model.Access

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
}