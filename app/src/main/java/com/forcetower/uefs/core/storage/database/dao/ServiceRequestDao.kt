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

package com.forcetower.uefs.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.forcetower.uefs.core.model.unes.ServiceRequest
import timber.log.Timber

@Dao
abstract class ServiceRequestDao {
    @Insert(onConflict = REPLACE)
    abstract fun insert(service: ServiceRequest)

    @Transaction
    open fun insertList(list: List<ServiceRequest>) {
        list.forEach {
            val existing = getSpecificDirect(it.date, it.service)
            if (existing != null && !existing.situation.equals(it.situation, ignoreCase = true)) {
                if (it.isAtStartState() || existing.isAtFinalState()) {
                    Timber.d("Can't downgrade!")
                } else {
                    existing.observation = it.observation
                    existing.situation = it.situation
                    existing.notify = 2
                    updateServiceRequest(existing)
                }
            } else if (existing == null) {
                if (it.isAtFinalState()) it.notify = 2
                insert(it)
            } else {
                Timber.d("Ignored ${it.service} at ${it.date} because no change was detected")
            }
        }
    }

    @Update(onConflict = IGNORE)
    abstract fun updateServiceRequest(service: ServiceRequest)

    @Query("SELECT * FROM ServiceRequest WHERE date = :date AND LOWER(service) = LOWER(:service) LIMIT 1")
    protected abstract fun getSpecificDirect(date: String, service: String): ServiceRequest?

    @Query("SELECT * FROM ServiceRequest WHERE notify = 1")
    abstract fun getCreatedDirect(): List<ServiceRequest>

    @Query("SELECT * FROM ServiceRequest WHERE notify = 2")
    abstract fun getStatusChangedDirect(): List<ServiceRequest>

    @Query("SELECT * FROM ServiceRequest")
    abstract fun getAll(): LiveData<List<ServiceRequest>>

    @Query("SELECT * FROM ServiceRequest WHERE LOWER(situation) = LOWER(:filter)")
    abstract fun getFiltered(filter: String): LiveData<List<ServiceRequest>>

    @Query("SELECT * FROM ServiceRequest WHERE LOWER(situation) <> 'atendido'")
    abstract fun getIncomplete(): LiveData<List<ServiceRequest>>

    @Query("SELECT * FROM ServiceRequest WHERE LOWER(situation) = 'atendido' OR LOWER(situation) = 'indeferido'")
    abstract fun getComplete(): LiveData<List<ServiceRequest>>

    @Query("UPDATE ServiceRequest SET notify = 0")
    abstract fun markAllNotified()
}