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
import com.forcetower.unes.core.model.AccessToken

@Dao
interface AccessTokenDao {
    @Insert(onConflict = REPLACE)
    fun insert(access: AccessToken)

    @Query("SELECT * FROM AccessToken LIMIT 1")
    fun getAccess(): LiveData<AccessToken?>

    @Query("SELECT * FROM AccessToken LIMIT 1")
    fun getAccessDirect(): AccessToken?

    @Query("DELETE FROM AccessToken")
    fun deleteAll()
}