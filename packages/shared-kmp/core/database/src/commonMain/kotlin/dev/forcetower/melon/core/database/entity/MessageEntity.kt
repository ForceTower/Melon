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
)
