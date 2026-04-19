package dev.forcetower.melon.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.forcetower.melon.core.database.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT value FROM Settings WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM Settings WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Upsert
    suspend fun put(entry: SettingsEntity)

    @Query("DELETE FROM Settings WHERE `key` = :key")
    suspend fun remove(key: String)

    @Query("DELETE FROM Settings")
    suspend fun clear()
}
