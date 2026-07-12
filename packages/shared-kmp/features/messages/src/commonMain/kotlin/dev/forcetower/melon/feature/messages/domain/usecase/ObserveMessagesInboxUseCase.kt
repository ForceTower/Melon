package dev.forcetower.melon.feature.messages.domain.usecase

import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.core.database.entity.MessageAttachmentEntity
import dev.forcetower.melon.core.database.entity.MessageEntity
import dev.forcetower.melon.core.database.entity.MessageScopeEntity
import dev.forcetower.melon.core.database.entity.MessageStateEntity
import dev.forcetower.melon.feature.messages.domain.internal.MessageOriginResolver
import dev.forcetower.melon.feature.messages.domain.internal.toMessageFeedSource
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedAttachmentKind
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedItem
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Inject
class ObserveMessagesInboxUseCase internal constructor(
    private val messageDao: MessageDao,
) {
    operator fun invoke(): Flow<List<MessageFeedItem>> = combine(
        messageDao.observeInbox(),
        messageDao.observeAllScopes(),
        messageDao.observeAllAttachments(),
        messageDao.observeAllStates(),
    ) { messages, scopes, attachments, states ->
        val scopesByMessage = scopes.groupBy { it.messageId }
        val attachmentsByMessage = attachments.groupBy { it.messageId }
        val stateByMessage = states.associateBy { it.messageId }
        messages.map { entity ->
            buildItem(
                entity,
                scopesByMessage[entity.id].orEmpty(),
                attachmentsByMessage[entity.id].orEmpty(),
                stateByMessage[entity.id],
            )
        }
    }

    private fun buildItem(
        entity: MessageEntity,
        scopes: List<MessageScopeEntity>,
        attachments: List<MessageAttachmentEntity>,
        state: MessageStateEntity?,
    ): MessageFeedItem {
        val source = entity.source.toMessageFeedSource()
        val origin = MessageOriginResolver.resolve(source, scopes)
        val disciplineScope = MessageOriginResolver.primaryDisciplineScope(scopes)
        val imageCount = attachments.count { it.kind.equals("image", ignoreCase = true) }
        return MessageFeedItem(
            id = entity.id,
            source = source,
            origin = origin,
            disciplineCode = disciplineScope?.disciplineCode,
            disciplineName = disciplineScope?.disciplineName,
            subject = entity.subject,
            content = entity.content,
            senderName = entity.senderName,
            senderType = entity.senderType,
            timestamp = entity.timestamp,
            isUnread = entity.read != true && state?.readAt == null,
            isStarred = entity.starred == true || state?.starred == true,
            attachmentCount = attachments.size,
            imageCount = imageCount,
        )
    }
}

// Exposed for the detail use case to share the kind mapping.
internal fun String.toMessageFeedAttachmentKind(): MessageFeedAttachmentKind =
    when (lowercase()) {
        "image" -> MessageFeedAttachmentKind.IMAGE
        "link" -> MessageFeedAttachmentKind.LINK
        "pdf" -> MessageFeedAttachmentKind.PDF
        "video" -> MessageFeedAttachmentKind.VIDEO
        else -> MessageFeedAttachmentKind.OTHER
    }
