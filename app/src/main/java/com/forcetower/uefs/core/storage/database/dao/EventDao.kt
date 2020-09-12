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
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.uefs.core.model.unes.Event

@Dao
abstract class EventDao {
    @Query("SELECT * FROM Event WHERE fakeTemp is null or fakeTemp = 0")
    abstract fun all(): LiveData<List<Event>>

    @WorkerThread
    @Transaction
    open fun insert(events: List<Event>) {
        deleteAll()
        internalInsert(events)
    }

    @Query("DELETE FROM Event WHERE sending = 0 or sending is null")
    protected abstract fun deleteAll()

    @Insert(onConflict = REPLACE)
    protected abstract fun internalInsert(events: List<Event>)

    @Query("UPDATE Event SET participating = :participating WHERE id = :id")
    abstract fun updateParticipatingStatus(id: Long, participating: Boolean)

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

    @WorkerThread
    @Query("UPDATE Event SET sending = :sending WHERE id = :id")
    abstract fun setSending(id: Long, sending: Int)

    @Query("DELETE FROM Event WHERE fakeTemp = 1 AND sending = 0")
    abstract fun clearTemps()

    @Query("DELETE FROM Event WHERE id = :id")
    abstract fun deleteSingle(id: Long)

    @Query("SELECT * FROM Event WHERE id = :eventId")
    abstract fun getDirect(eventId: Long): Event?

    @Query("UPDATE Event SET imageUrl = :link WHERE id = :id")
    abstract fun updateImageUrl(id: Long, link: String)

    @Query("UPDATE Event SET approved = :approved WHERE id = :id")
    abstract fun setApproved(id: Long, approved: Boolean)
}
