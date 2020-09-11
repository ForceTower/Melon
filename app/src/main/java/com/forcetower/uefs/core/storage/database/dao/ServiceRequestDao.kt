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

    @Query("DELETE FROM ServiceRequest")
    abstract fun deleteAll()
}
