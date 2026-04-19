package dev.forcetower.melon.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.forcetower.melon.core.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM User LIMIT 1")
    fun observeCurrent(): Flow<UserEntity?>

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("DELETE FROM User")
    suspend fun clear()
}
