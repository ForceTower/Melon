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
    content = if (source == "upstream") content.cleanUpstreamContent() else content,
    senderName = senderName,
    senderType = senderType,
    timestamp = timestamp,
    createdAt = createdAt,
)

// SAGRES occasionally emits content where real line breaks were serialized
// as literal two-char sequences ("\\n", "\\r") — a round-trip bug inherited
// from the legacy scraper. Mirrors the legacy v1 Android fix (unescape at
// insert time so every downstream reader sees the same clean text).
// Scoped to upstream so app-authored content passes through untouched.
private fun String.cleanUpstreamContent(): String =
    replace("\\n", "\n").replace("\\r", "\r")

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
