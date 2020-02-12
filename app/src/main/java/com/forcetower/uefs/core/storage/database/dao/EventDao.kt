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
import com.forcetower.uefs.core.model.unes.Event

@Dao
abstract class EventDao {
    @Query("SELECT * FROM Event WHERE fakeTemp != 1")
    abstract fun all(): LiveData<List<Event>>

    @Insert(onConflict = REPLACE)
    abstract suspend fun insert(events: List<Event>)

    @Query("UPDATE Event SET participating = :participating WHERE id = :id")
    abstract suspend fun updateParticipatingStatus(id: Long, participating: Boolean)

    @Query("SELECT * FROM Event WHERE id = :eventId")
    abstract fun get(eventId: Long): LiveData<Event>

    @Transaction
    open suspend fun insertSingle(insert: Event): Long {
        val event = selectHighestId()
        val id = (event?.id ?: 0) + 2000
        val copy = insert.copy(id = id)
        return internalInsertSingle(copy)
    }

    @Query("SELECT * FROM Event ORDER BY id DESC LIMIT 1")
    protected abstract fun selectHighestId(): Event?

    @Insert(onConflict = REPLACE)
    protected abstract fun internalInsertSingle(event: Event): Long
}