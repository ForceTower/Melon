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
}
