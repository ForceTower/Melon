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

    @Query("SELECT * FROM User LIMIT 1")
    suspend fun getCurrent(): UserEntity?

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("UPDATE User SET name = :name, imageUrl = :imageUrl, email = :email WHERE id = :id")
    suspend fun updateProfile(id: String, name: String, imageUrl: String?, email: String?)

    @Query("DELETE FROM User")
    suspend fun clear()
}
