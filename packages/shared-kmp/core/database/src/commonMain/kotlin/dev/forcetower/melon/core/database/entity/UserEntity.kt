package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "User")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String?,
)
