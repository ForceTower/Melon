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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.forcetower.uefs.core.model.unes.UserSession

@Dao
abstract class UserSessionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(session: UserSession)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun update(session: UserSession)

    @Query("SELECT * FROM UserSession")
    abstract fun getAll(): List<UserSession>

    @Query("SELECT * FROM UserSession ORDER BY started DESC LIMIT 1")
    abstract fun getLatestSession(): UserSession?

    @Query("UPDATE UserSession SET lastInteraction = :time WHERE uid = :uid")
    abstract fun updateLastInteraction(uid: String, time: Long)

    @Query("SELECT * FROM UserSession WHERE synced = 0")
    abstract fun getUnsyncedSessions(): List<UserSession>

    @Query("UPDATE UserSession SET synced = 1 WHERE uid = :session")
    abstract fun markSyncedSession(session: String)

    @Query("UPDATE UserSession SET clickedAd = :clicked WHERE uid = :session")
    abstract fun updateClickedAd(session: String, clicked: Int)

    @Query("UPDATE UserSession SET impressionAd = :impression WHERE uid = :session")
    abstract fun updateAdImpression(session: String, impression: Int)

    @Query("DELETE FROM UserSession WHERE synced = 1")
    abstract fun removeSyncedSessions()
}
