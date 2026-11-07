package dev.forcetower.melon.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

// Single-student mobile: we drop the server's composite (message_id, student_id)
// key and key purely on messageId. Read/starred state is local-first: the
// device acks it right away, and the inbox sync replays star changes the
// server has not acknowledged yet.
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
    // True while `starred` is a local change the server has not acknowledged:
    // it then wins over Message.starred. Once acknowledged, Message.starred
    // alone decides, so a change made on another device shows up here.
    @ColumnInfo(defaultValue = "0") val starPending: Boolean = false,
)
