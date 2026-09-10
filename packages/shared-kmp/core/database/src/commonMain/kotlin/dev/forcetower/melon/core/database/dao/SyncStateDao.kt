package dev.forcetower.melon.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import dev.forcetower.melon.core.database.entity.SyncStateEntity

@Dao
interface SyncStateDao {
    @Query("SELECT value FROM SyncState WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Upsert
    suspend fun put(entry: SyncStateEntity)

    @Query("DELETE FROM SyncState WHERE `key` = :key")
    suspend fun remove(key: String)

    @Query("DELETE FROM SyncState")
    suspend fun clear()
}
