package dev.forcetower.melon.core.sync.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class MessagePageResponse(
    val messages: List<MessageDto>,
    val nextCursor: String?,
)

@Serializable
internal data class MessageDto(
    val id: String,
    // "upstream" | "app"
    val source: String,
    val platformId: String?,
    val subject: String?,
    val content: String,
    val senderName: String,
    val senderType: Int?,
    // ISO-8601 datetime; persisted as String in Message.timestamp.
    val timestamp: String,
    val createdAt: String,
    val read: Boolean,
    val readAt: String?,
    val starred: Boolean,
    val scopes: List<MessageScopeDto>,
    val attachments: List<MessageAttachmentDto>,
)

@Serializable
internal data class MessageScopeDto(
    val id: String,
    // university | coordination | course | class | personal | list
    val scope: String,
    val classId: String?,
    val courseId: String?,
    val studentId: String?,
    val disciplineCode: String?,
    val disciplineName: String?,
    val platformScopeId: String?,
)

@Serializable
internal data class MessageAttachmentDto(
    val id: String,
    // image | link | pdf | video | other
    val kind: String,
    val name: String?,
    val url: String,
    val position: Int,
)
