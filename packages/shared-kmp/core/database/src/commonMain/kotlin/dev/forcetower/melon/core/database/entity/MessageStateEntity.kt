package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// Single-student mobile: we drop the server's composite (message_id, student_id)
// key and key purely on messageId. Read/starred state is local-first + replayed
// via the pending_mutation queue.
@Entity(
    tableName = "MessageState",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MessageStateEntity(
    @PrimaryKey val messageId: String,
    val readAt: String?,
    val starred: Boolean,
    val updatedAt: String,
)
