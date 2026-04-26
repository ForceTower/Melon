package dev.forcetower.unes.ui.feature.messages

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.forcetower.unes.R
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.datetime.Instant
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedAttachment as KmpMessageFeedAttachment
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedAttachmentKind as KmpMessageFeedAttachmentKind
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedDetail as KmpMessageFeedDetail
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedItem as KmpMessageFeedItem
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedOrigin as KmpMessageFeedOrigin

// KMP feed types → presentation `Message` projection. Mirrors
// `apps/ios/UNES/Features/Messages/Models/MessageMapping.swift`. Fields the
// DB doesn't track (moduleId, link title/host, attachment size, sender role)
// are either left null or derived here from what we *do* have.
//
// `MessageOrigin.Module` is intentionally never emitted — the KMP feed has
// no module origin, but the UI keeps the case defined for future module
// inboxes (intercâmbio, biblioteca, RU).

// Pre-resolved role strings — looked up once per composition so the mapper
// stays a pure (Context-free) function below. Discipline rows use the KMP
// `disciplineName` directly when present and fall back to this default.
internal data class MessageRoleStrings(
    val disciplineDefault: String,
    val secretariat: String,
    val campus: String,
    val app: String,
    val module: String,
    val direct: String,
)

internal fun MessageRoleStrings(context: Context): MessageRoleStrings = MessageRoleStrings(
    disciplineDefault = context.getString(R.string.messages_role_discipline_default),
    secretariat = context.getString(R.string.messages_role_secretariat),
    campus = context.getString(R.string.messages_role_campus),
    app = context.getString(R.string.messages_role_app),
    module = context.getString(R.string.messages_role_module),
    direct = context.getString(R.string.messages_role_direct),
)

@Composable
internal fun rememberMessageRoleStrings(): MessageRoleStrings {
    val context = LocalContext.current
    return remember(context) { MessageRoleStrings(context) }
}

internal fun KmpMessageFeedItem.toUi(roles: MessageRoleStrings): Message {
    val mappedOrigin = mapOrigin(origin)
    return Message(
        id = id,
        origin = mappedOrigin,
        sender = MessageSender(
            name = senderName,
            role = roleFor(mappedOrigin, disciplineName, roles),
        ),
        body = content,
        receivedAt = parseTimestamp(timestamp),
        disciplineCode = disciplineCode.takeIf { mappedOrigin == MessageOrigin.Discipline },
        moduleId = null,
        subject = subject?.normalizedSubject(),
        preview = null,
        unread = isUnread,
        starred = isStarred,
        attachments = emptyList(),
    )
}

internal fun KmpMessageFeedDetail.toUi(roles: MessageRoleStrings): Message {
    val mappedOrigin = mapOrigin(origin)
    return Message(
        id = id,
        origin = mappedOrigin,
        sender = MessageSender(
            name = senderName,
            role = roleFor(mappedOrigin, disciplineName, roles),
        ),
        body = content,
        receivedAt = parseTimestamp(timestamp),
        disciplineCode = disciplineCode.takeIf { mappedOrigin == MessageOrigin.Discipline },
        moduleId = null,
        subject = subject?.normalizedSubject(),
        preview = null,
        unread = isUnread,
        starred = isStarred,
        attachments = attachments.map(::mapAttachment),
    )
}

private fun mapOrigin(raw: KmpMessageFeedOrigin): MessageOrigin = when (raw) {
    KmpMessageFeedOrigin.DISCIPLINE -> MessageOrigin.Discipline
    KmpMessageFeedOrigin.SECRETARIAT -> MessageOrigin.Secretariat
    KmpMessageFeedOrigin.CAMPUS -> MessageOrigin.Campus
    KmpMessageFeedOrigin.APP -> MessageOrigin.App
    KmpMessageFeedOrigin.DIRECT -> MessageOrigin.Direct
}

private fun mapAttachment(raw: KmpMessageFeedAttachment): MessageAttachment {
    val kind = mapKind(raw.kind)
    return when (kind) {
        MessageAttachmentKind.Link -> {
            val host = hostFor(raw.url)
            MessageAttachment(
                kind = MessageAttachmentKind.Link,
                title = raw.name ?: host ?: raw.url,
                host = host,
                url = raw.url,
            )
        }
        MessageAttachmentKind.Image -> MessageAttachment(
            kind = MessageAttachmentKind.Image,
            url = raw.url,
        )
        else -> MessageAttachment(
            kind = kind,
            name = raw.name ?: lastPathComponent(raw.url) ?: raw.url,
            url = raw.url,
        )
    }
}

// Fixtures carry a `.slides` kind that upstream/DB never emits. Video is
// rare; fold into the generic file tile — matches iOS MessageMapping.
private fun mapKind(raw: KmpMessageFeedAttachmentKind): MessageAttachmentKind = when (raw) {
    KmpMessageFeedAttachmentKind.IMAGE -> MessageAttachmentKind.Image
    KmpMessageFeedAttachmentKind.LINK -> MessageAttachmentKind.Link
    KmpMessageFeedAttachmentKind.PDF -> MessageAttachmentKind.Pdf
    KmpMessageFeedAttachmentKind.VIDEO -> MessageAttachmentKind.Other
    KmpMessageFeedAttachmentKind.OTHER -> MessageAttachmentKind.Other
}

private fun roleFor(
    origin: MessageOrigin,
    disciplineName: String?,
    roles: MessageRoleStrings,
): String = when (origin) {
    MessageOrigin.Discipline -> disciplineName?.takeIf { it.isNotBlank() } ?: roles.disciplineDefault
    MessageOrigin.Secretariat -> roles.secretariat
    MessageOrigin.Campus -> roles.campus
    MessageOrigin.App -> roles.app
    MessageOrigin.Module -> roles.module
    MessageOrigin.Direct -> roles.direct
}

private fun String.normalizedSubject(): String? = trim().takeIf { it.isNotEmpty() }

private fun hostFor(url: String): String? = runCatching {
    URI(url).host?.takeIf { it.isNotBlank() }?.removePrefix("www.")
}.getOrNull()

private fun lastPathComponent(url: String): String? = runCatching {
    URI(url).path?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
}.getOrNull()

// ISO-8601 from Postgres/Ktor comes through in two flavors: with fractional
// seconds ("2025-11-03T14:22:18.123Z") and without ("2025-11-03T14:22:18Z").
// `kotlinx.datetime.Instant.parse` accepts both, so a single attempt covers
// the iOS dual-formatter fallback. On failure (truly malformed input) we
// fall back to the current local time so the UI never crashes.
private fun parseTimestamp(iso: String): LocalDateTime {
    val instant = runCatching { Instant.parse(iso) }.getOrNull()
        ?: return LocalDateTime.now()
    return LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()),
        ZoneId.systemDefault(),
    )
}
