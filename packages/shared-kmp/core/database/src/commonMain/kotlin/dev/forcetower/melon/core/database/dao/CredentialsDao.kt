package dev.forcetower.melon.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.forcetower.melon.core.database.entity.CredentialsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialsDao {
    @Query("SELECT * FROM Credentials LIMIT 1")
    fun observeCurrent(): Flow<CredentialsEntity?>

    @Query("SELECT * FROM Credentials LIMIT 1")
    suspend fun getCurrent(): CredentialsEntity?

    @Upsert
    suspend fun upsert(credentials: CredentialsEntity)

    @Query("DELETE FROM Credentials")
    suspend fun clear()
}
