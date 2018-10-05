/*
 * Copyright (c) 2018.
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

package com.forcetower.uefs.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.forcetower.uefs.core.model.unes.SagresDocument

@Dao
interface DocumentDao {
    @Insert(onConflict = IGNORE)
    fun insert(document: SagresDocument)

    @Update(onConflict = REPLACE)
    fun replace(document: SagresDocument)

    @Query("UPDATE SagresDocument SET downloaded = :downloaded WHERE type = :type")
    fun updateDownloaded(downloaded: Boolean, type: String)

    @Query("UPDATE SagresDocument SET downloading = :downloading WHERE type = :type")
    fun updateDownloading(downloading: Boolean, type: String)

    @Query("SELECT * FROM SagresDocument WHERE type = :type LIMIT 1")
    fun getDocumentDirect(type: String): SagresDocument?

    @Query("SELECT * FROM SagresDocument")
    fun getDocuments(): LiveData<List<SagresDocument>>

    @Query("SELECT * FROM SagresDocument")
    fun getDocumentsDirect(): List<SagresDocument>
}