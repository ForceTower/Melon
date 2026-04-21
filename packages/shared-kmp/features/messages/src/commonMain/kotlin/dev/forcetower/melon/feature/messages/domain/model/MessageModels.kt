package dev.forcetower.melon.feature.messages.domain.model

enum class MessageFeedSource { UPSTREAM, APP }

// High-level origin the UI tints a row / swatch / detail glow from. Derived
// from (source, scopes) via `MessageOriginResolver` so the native code never
// has to look at scope strings.
enum class MessageFeedOrigin { DISCIPLINE, SECRETARIAT, CAMPUS, APP, DIRECT }

enum class MessageFeedAttachmentKind { IMAGE, LINK, PDF, VIDEO, OTHER }

data class MessageFeedAttachment(
    val id: String,
    val kind: MessageFeedAttachmentKind,
    val name: String?,
    val url: String,
    val position: Int,
)

data class MessageFeedItem(
    val id: String,
    val source: MessageFeedSource,
    val origin: MessageFeedOrigin,
    val disciplineCode: String?,
    val disciplineName: String?,
    val subject: String?,
    val content: String,
    val senderName: String,
    val senderType: Int?,
    val timestamp: String,
    val isUnread: Boolean,
    val isStarred: Boolean,
    val attachmentCount: Int,
    val imageCount: Int,
)

data class MessageFeedDetail(
    val id: String,
    val source: MessageFeedSource,
    val origin: MessageFeedOrigin,
    val disciplineCode: String?,
    val disciplineName: String?,
    val subject: String?,
    val content: String,
    val senderName: String,
    val senderType: Int?,
    val timestamp: String,
    val isUnread: Boolean,
    val isStarred: Boolean,
    val attachments: List<MessageFeedAttachment>,
)
