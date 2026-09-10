package dev.forcetower.melon.core.database.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

// Operation log for local-first writes (e.g., mark-message-read). The drainer
// reads oldest-first, replays each against the server with `id` as the
// Idempotency-Key, deletes on success.
@Entity(
    tableName = "PendingMutation",
    indices = [Index("createdAt")],
)
data class PendingMutationEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val payloadJson: String,
    val createdAt: String,
    val attempts: Int,
    val lastError: String?,
    val lastAttemptedAt: String?,
)
