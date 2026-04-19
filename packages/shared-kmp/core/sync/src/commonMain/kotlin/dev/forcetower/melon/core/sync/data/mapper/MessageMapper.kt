package dev.forcetower.melon.core.sync.data.mapper

import dev.forcetower.melon.core.database.entity.MessageAttachmentEntity
import dev.forcetower.melon.core.database.entity.MessageEntity
import dev.forcetower.melon.core.database.entity.MessageScopeEntity
import dev.forcetower.melon.core.sync.data.dto.MessageAttachmentDto
import dev.forcetower.melon.core.sync.data.dto.MessageDto
import dev.forcetower.melon.core.sync.data.dto.MessageScopeDto

internal fun MessageDto.toEntity(): MessageEntity = MessageEntity(
    id = id,
    platformId = platformId,
    source = source,
    subject = subject,
    content = content,
    senderName = senderName,
    senderType = senderType,
    timestamp = timestamp,
    createdAt = createdAt,
)

internal fun MessageScopeDto.toEntity(messageId: String): MessageScopeEntity = MessageScopeEntity(
    id = id,
    messageId = messageId,
    scope = scope,
    classId = classId,
    courseId = courseId,
    studentId = studentId,
    platformScopeId = platformScopeId,
    disciplineCode = disciplineCode,
    disciplineName = disciplineName,
)

internal fun MessageAttachmentDto.toEntity(messageId: String): MessageAttachmentEntity = MessageAttachmentEntity(
    id = id,
    messageId = messageId,
    kind = kind,
    name = name,
    url = url,
    position = position,
)
