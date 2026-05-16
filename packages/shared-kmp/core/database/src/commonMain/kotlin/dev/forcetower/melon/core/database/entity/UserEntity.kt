package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Login writes {id, name, imageUrl}; sync/profile tops up `email`. On logout +
// re-login, email goes back to null until the first profile sync — acceptable
// for v1 since the next sync tick fills it.
@Entity(tableName = "User")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String?,
    val email: String? = null,
)
