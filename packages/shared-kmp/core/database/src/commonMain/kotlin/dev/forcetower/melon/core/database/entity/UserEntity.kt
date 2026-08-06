package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Login writes {id, name, imageUrl}; sync/profile tops up `email` and
// `alternateName`. On logout + re-login, both go back to null until the first
// profile sync — acceptable for v1 since the next sync tick fills them.
//
// `alternateName` is the profile-customization display name ("apelido") the
// user set through PATCH api/me/name; `name` stays the upstream registry name
// that documents keep using. Display rule everywhere: alternateName ?: name.
@Entity(tableName = "User")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String?,
    val email: String? = null,
    val alternateName: String? = null,
)
