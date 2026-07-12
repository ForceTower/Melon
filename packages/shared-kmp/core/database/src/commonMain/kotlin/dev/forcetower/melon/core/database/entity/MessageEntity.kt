package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Message",
    indices = [Index(value = ["timestamp", "id"], orders = [Index.Order.DESC, Index.Order.DESC])],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val platformId: String?,
    val source: String, // "upstream" | "app"
    val subject: String?,
    val content: String,
    val senderName: String,
    val senderType: Int?,
    val timestamp: String,
    val createdAt: String,
    // Server-merged read/starred across all linked devices, mirrored at sync
    // time. Display merges them with the local MessageState overlay (matches
    // iOS `MirrorStore.messageItem`): unread = read != true && readAt == null,
    // starred = starred == true || overlay.starred.
    val read: Boolean?,
    val starred: Boolean?,
)
