package dev.forcetower.unes.ui.feature.messages

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.overview.ColorFor
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.floor

// Local UI models for the messages inbox. Mirrors `MessageModels.swift` on
// iOS (`apps/ios/UNES/Features/Messages/Models/MessageModels.swift`) and
// `screens-messages-data.jsx` from the design bundle.
//
// Each kind gets distinct visual treatment on the list row swatch and an
// accent color that tints the detail screen's ambient glow.
internal enum class MessageOrigin {
    Discipline,   // from a specific enrolled discipline (sender is the prof)
    Secretariat,  // from Secretaria Acadêmica
    Campus,       // campus-wide announcement
    App,          // from the UNES team
    Module,       // from an extra module (intercâmbio, biblioteca, RU)
    Direct,       // direct message addressed to THIS student personally
}

internal enum class MessageAttachmentKind(val label: String) {
    Pdf("PDF"),
    Slides("SLIDES"),
    Image("IMAGE"),
    Link("LINK"),
    Other("FILE"),
}

internal data class MessageAttachment(
    val kind: MessageAttachmentKind,
    val name: String? = null,
    val size: String? = null,
    val title: String? = null,
    val host: String? = null,
    val url: String? = null,
)

internal data class MessageSender(val name: String, val role: String)

internal data class Message(
    val id: String,
    val origin: MessageOrigin,
    val sender: MessageSender,
    val body: String,
    val receivedAt: LocalDateTime,
    val disciplineCode: String? = null,
    val moduleId: String? = null,
    val subject: String? = null,
    val preview: String? = null,
    val unread: Boolean = false,
    val starred: Boolean = false,
    val attachments: List<MessageAttachment> = emptyList(),
)

// Visual metadata attached to a message based on its origin. The accent
// `color` tints the swatch, the meta line on rows, and the detail glow.
internal data class MessageOriginMeta(
    val label: String,
    val color: Color,
    @StringRes val kindRes: Int,
)

@Composable
@ReadOnlyComposable
internal fun originMeta(message: Message): MessageOriginMeta {
    val palette = MaterialTheme.melon.palette
    val outline = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary
    val tealApp = palette.teal
    return when (message.origin) {
        MessageOrigin.Discipline -> MessageOriginMeta(
            label = message.disciplineCode.orEmpty(),
            color = message.disciplineCode?.let { ColorFor.discipline(palette, it) } ?: outline,
            kindRes = R.string.messages_origin_kind_discipline,
        )
        MessageOrigin.Secretariat -> MessageOriginMeta(
            label = "SEC",
            color = MessagesPalette.Secretariat,
            kindRes = R.string.messages_origin_kind_secretariat,
        )
        MessageOrigin.Campus -> MessageOriginMeta(
            label = "UEFS",
            color = MessagesPalette.Campus,
            kindRes = R.string.messages_origin_kind_campus,
        )
        MessageOrigin.App -> MessageOriginMeta(
            label = "UNES",
            color = tealApp,
            kindRes = R.string.messages_origin_kind_app,
        )
        MessageOrigin.Module -> {
            val (label, color) = moduleMeta(message.moduleId)
            MessageOriginMeta(label = label, color = color, kindRes = R.string.messages_origin_kind_module)
        }
        MessageOrigin.Direct -> MessageOriginMeta(
            label = "·",
            color = accent,
            kindRes = R.string.messages_origin_kind_direct,
        )
    }
}

private fun moduleMeta(moduleId: String?): Pair<String, Color> = when (moduleId) {
    "intercambio" -> "INTER" to MessagesPalette.ModuleIntercambio
    "biblioteca" -> "BIB" to MessagesPalette.ModuleBiblioteca
    "ru" -> "RU" to MessagesPalette.ModuleRu
    else -> "MOD" to MessagesPalette.ModuleDefault
}

// Colors that don't sit cleanly on `MaterialTheme.melon` — secretariat grey,
// campus amber-ish, and the module hues. Mirrors the literals from
// `originMeta()` in `screens-messages-data.jsx`.
private object MessagesPalette {
    val Secretariat = Color(0xFF6B5E70)
    val Campus = Color(0xFFD9852E)
    val ModuleIntercambio = Color(0xFF6B4B9C)
    val ModuleBiblioteca = Color(0xFF5C8C3E)
    val ModuleRu = Color(0xFFC37A4A)
    val ModuleDefault = Color(0xFF6B5E70)
}

// Filter chips across the top of the inbox.
internal enum class MessageFilter(@StringRes val labelRes: Int) {
    All(R.string.messages_filter_all),
    Unread(R.string.messages_filter_unread),
    Starred(R.string.messages_filter_starred),
    Disc(R.string.messages_filter_disciplines),
    Univ(R.string.messages_filter_university),
    App(R.string.messages_filter_app);

    fun matches(m: Message): Boolean = when (this) {
        All -> true
        Unread -> m.unread
        Starred -> m.starred
        Disc -> m.origin == MessageOrigin.Discipline || m.origin == MessageOrigin.Direct
        Univ -> m.origin == MessageOrigin.Secretariat || m.origin == MessageOrigin.Campus
        App -> m.origin == MessageOrigin.App || m.origin == MessageOrigin.Module
    }
}

// Date bucket headers in the list. Order matches iOS `MessageDate.Bucket`.
internal enum class MessageBucket(@StringRes val labelRes: Int) {
    Today(R.string.messages_bucket_today),
    Yesterday(R.string.messages_bucket_yesterday),
    ThisWeek(R.string.messages_bucket_this_week),
    ThisMonth(R.string.messages_bucket_this_month),
    Older(R.string.messages_bucket_older),
}

// "Today" for the demo bucketing matches the JSX prototype/iOS fixtures —
// 2026-04-18 13:20 — so the seed messages land in the same buckets across
// every platform. When the live feed hits, this becomes `LocalDateTime.now()`.
internal val MessagesNow: LocalDateTime = LocalDateTime.of(2026, 4, 18, 13, 20)

internal fun bucketOf(received: LocalDateTime, now: LocalDateTime = MessagesNow): MessageBucket {
    val days = floor(secondsBetween(received, now) / 86_400.0).toInt()
    return when {
        days <= 0 -> MessageBucket.Today
        days == 1 -> MessageBucket.Yesterday
        days <= 7 -> MessageBucket.ThisWeek
        days <= 31 -> MessageBucket.ThisMonth
        else -> MessageBucket.Older
    }
}

// Short relative timestamp shown on each row. Mirrors `relTime()` in
// `screens-messages-data.jsx` and `MessageDate.relativeTime` on iOS. The
// `Literal` variant carries a pre-built short date ("10 abr") because
// Portuguese month abbreviations don't round-trip through CLDR patterns.
internal sealed class RelativeTime {
    data class Resource(@StringRes val res: Int, val arg: Int? = null) : RelativeTime()
    data class Literal(val text: String) : RelativeTime()
}

internal fun relativeTime(received: LocalDateTime, now: LocalDateTime = MessagesNow): RelativeTime {
    val mins = floor(secondsBetween(received, now) / 60.0).toInt()
    if (mins < 1) return RelativeTime.Resource(R.string.messages_rel_time_now)
    if (mins < 60) return RelativeTime.Resource(R.string.messages_rel_time_minutes, arg = mins)
    val hrs = mins / 60
    if (hrs < 24) return RelativeTime.Resource(R.string.messages_rel_time_hours, arg = hrs)
    val days = hrs / 24
    if (days == 1) return RelativeTime.Resource(R.string.messages_rel_time_yesterday)
    if (days <= 6) return RelativeTime.Resource(R.string.messages_rel_time_days, arg = days)
    val months = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")
    return RelativeTime.Literal("${received.dayOfMonth} ${months[received.monthValue - 1]}")
}

// Long form used on the detail screen — "18 de abril de 2026 · 09:14".
internal fun fullTime(received: LocalDateTime): String {
    val months = listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    )
    val hh = received.hour.toString().padStart(2, '0')
    val mm = received.minute.toString().padStart(2, '0')
    return "${received.dayOfMonth} de ${months[received.monthValue - 1]} de ${received.year} · $hh:$mm"
}

private fun secondsBetween(start: LocalDateTime, end: LocalDateTime): Double {
    val zone = ZoneId.systemDefault()
    return (end.atZone(zone).toEpochSecond() - start.atZone(zone).toEpochSecond()).toDouble()
}
