package dev.forcetower.unes.ui.feature.messages

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.floor

// Local UI models for the messages inbox. Mirrors `MessageModels.swift` on
// iOS (`apps/ios/UNES/Features/Messages/Models/MessageModels.swift`); visuals
// follow the dc `UNES Mensagens - Android` redesign, which tints every row by
// one of three top-level categories (Disciplinas · Universidade · App).
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

// Top-level category the dc redesign groups origins into — drives the tonal
// avatar hue, the hero segmented bar, and the legend.
internal enum class MessageCategory(@StringRes val labelRes: Int) {
    Disciplines(R.string.messages_category_disciplines),
    University(R.string.messages_category_university),
    App(R.string.messages_category_app),
}

internal val Message.category: MessageCategory
    get() = when (origin) {
        MessageOrigin.Discipline, MessageOrigin.Direct -> MessageCategory.Disciplines
        MessageOrigin.Secretariat, MessageOrigin.Campus -> MessageCategory.University
        MessageOrigin.App, MessageOrigin.Module -> MessageCategory.App
    }

// Category hue map from the dc prototype: Disciplinas = jade,
// Universidade = amber (status.warn), App = violet.
@Composable
@ReadOnlyComposable
internal fun categoryColor(category: MessageCategory): Color = when (category) {
    MessageCategory.Disciplines -> MaterialTheme.melon.palette.jade
    MessageCategory.University -> MaterialTheme.melon.status.warn
    MessageCategory.App -> MaterialTheme.melon.palette.violet
}

// Leading avatar glyph — per origin, so a secretaria row still reads
// differently from a comunicado even though both tint amber.
internal fun originIcon(origin: MessageOrigin): ImageVector = when (origin) {
    MessageOrigin.Discipline -> Icons.AutoMirrored.Filled.MenuBook
    MessageOrigin.Secretariat -> Icons.Filled.AccountBalance
    MessageOrigin.Campus -> Icons.Filled.Campaign
    MessageOrigin.App -> Icons.Filled.AutoAwesome
    MessageOrigin.Module -> Icons.Filled.Widgets
    MessageOrigin.Direct -> Icons.Filled.Person
}

@StringRes
internal fun originKindRes(origin: MessageOrigin): Int = when (origin) {
    MessageOrigin.Discipline -> R.string.messages_origin_kind_discipline
    MessageOrigin.Secretariat -> R.string.messages_origin_kind_secretariat
    MessageOrigin.Campus -> R.string.messages_origin_kind_campus
    MessageOrigin.App -> R.string.messages_origin_kind_app
    MessageOrigin.Module -> R.string.messages_origin_kind_module
    MessageOrigin.Direct -> R.string.messages_origin_kind_direct
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
        Disc -> m.category == MessageCategory.Disciplines
        Univ -> m.category == MessageCategory.University
        App -> m.category == MessageCategory.App
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

internal fun bucketOf(received: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): MessageBucket {
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
// `Literal` variant carries a pre-built short date ("10 abr").
internal sealed class RelativeTime {
    data class Resource(@StringRes val res: Int, val arg: Int? = null) : RelativeTime()
    data class Literal(val text: String) : RelativeTime()
}

private val ShortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

internal fun relativeTime(received: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): RelativeTime {
    val mins = floor(secondsBetween(received, now) / 60.0).toInt()
    if (mins < 1) return RelativeTime.Resource(R.string.messages_rel_time_now)
    if (mins < 60) return RelativeTime.Resource(R.string.messages_rel_time_minutes, arg = mins)
    val hrs = mins / 60
    if (hrs < 24) return RelativeTime.Resource(R.string.messages_rel_time_hours, arg = hrs)
    val days = hrs / 24
    if (days == 1) return RelativeTime.Resource(R.string.messages_rel_time_yesterday)
    if (days <= 6) return RelativeTime.Resource(R.string.messages_rel_time_days, arg = days)
    return RelativeTime.Literal(ShortDateFormatter.format(received))
}

private val LongDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault())

// Long form used on the detail screen — "18 de abril de 2026 · 09:14".
internal fun fullTime(received: LocalDateTime): String {
    val hh = received.hour.toString().padStart(2, '0')
    val mm = received.minute.toString().padStart(2, '0')
    return "${LongDateFormatter.format(received)} · $hh:$mm"
}

private fun secondsBetween(start: LocalDateTime, end: LocalDateTime): Double {
    val zone = ZoneId.systemDefault()
    return (end.atZone(zone).toEpochSecond() - start.atZone(zone).toEpochSecond()).toDouble()
}
