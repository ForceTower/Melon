package dev.forcetower.melon.feature.messages.domain.usecase

import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.core.database.entity.MessageAttachmentEntity
import dev.forcetower.melon.core.database.entity.MessageEntity
import dev.forcetower.melon.core.database.entity.MessageScopeEntity
import dev.forcetower.melon.core.database.entity.MessageStateEntity
import dev.forcetower.melon.feature.messages.domain.internal.MessageOriginResolver
import dev.forcetower.melon.feature.messages.domain.internal.toMessageFeedSource
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedAttachment
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedDetail
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Inject
class ObserveMessageDetailUseCase internal constructor(
    private val messageDao: MessageDao,
) {
    operator fun invoke(messageId: String): Flow<MessageFeedDetail?> = combine(
        messageDao.observeMessage(messageId),
        messageDao.observeScopesFor(messageId),
        messageDao.observeAttachmentsFor(messageId),
        messageDao.observeStates(listOf(messageId)),
    ) { message, scopes, attachments, states ->
        message?.let { build(it, scopes, attachments, states.firstOrNull()) }
    }

    private fun build(
        entity: MessageEntity,
        scopes: List<MessageScopeEntity>,
        attachments: List<MessageAttachmentEntity>,
        state: MessageStateEntity?,
    ): MessageFeedDetail {
        val source = entity.source.toMessageFeedSource()
        val origin = MessageOriginResolver.resolve(source, scopes)
        val disciplineScope = MessageOriginResolver.primaryDisciplineScope(scopes)
        return MessageFeedDetail(
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
            isUnread = state?.readAt == null,
            isStarred = state?.starred == true,
            attachments = attachments.map { it.toModel() },
        )
    }

    private fun MessageAttachmentEntity.toModel(): MessageFeedAttachment = MessageFeedAttachment(
        id = id,
        kind = kind.toMessageFeedAttachmentKind(),
        name = name,
        url = url,
        position = position,
    )
}
