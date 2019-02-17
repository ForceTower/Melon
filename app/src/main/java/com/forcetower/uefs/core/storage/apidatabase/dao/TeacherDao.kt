/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.uefs.core.storage.apidatabase.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.forcetower.uefs.core.model.api.UTeacher

@Dao
abstract class TeacherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(teachers: List<UTeacher>)

    open fun query(query: String?): LiveData<List<UTeacher>> {
        return if (query.isNullOrBlank()) {
            getAll()
        } else {
            val string = "%${query.toUpperCase()}%"
            doQuery(string)
        }
    }

    // TODO This should be changed to a FTS table for fast scans
    @Query("SELECT * FROM UTeacher WHERE UPPER(name) LIKE :query ORDER BY name")
    protected abstract fun doQuery(query: String): LiveData<List<UTeacher>>

    @Query("SELECT * FROM UTeacher ORDER BY name")
    abstract fun getAll(): LiveData<List<UTeacher>>
}