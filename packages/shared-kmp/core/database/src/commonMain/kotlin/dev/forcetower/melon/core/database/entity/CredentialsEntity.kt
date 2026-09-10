package dev.forcetower.melon.core.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

// Single-row store for the upstream login credentials the user typed in. The
// row is upserted by `SessionStore.persist` and wiped on logout. PK doubles as
// the FK to `User.id` with cascading delete so wiping the user row also drops
// the credentials, keeping logout teardown order-independent.
@Entity(
    tableName = "Credentials",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CredentialsEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val password: String,
    // ISO-8601 timestamp of the most recent successful login that wrote this
    // row. Useful for diagnostics + future silent-reauth UX.
    val updatedAt: String,
)
