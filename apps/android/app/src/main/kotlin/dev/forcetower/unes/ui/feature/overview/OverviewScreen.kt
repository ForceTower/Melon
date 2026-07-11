package dev.forcetower.unes.ui.feature.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeInOnAppear
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.ui.feature.overview.components.FinalStretchCard
import dev.forcetower.unes.ui.feature.overview.components.HeroCard
import dev.forcetower.unes.ui.feature.overview.components.MessagesPreview
import dev.forcetower.unes.ui.feature.overview.components.OverviewHeader
import dev.forcetower.unes.ui.feature.overview.components.TodayTimeline
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Instant
import dev.forcetower.melon.feature.overview.domain.model.OverviewClassState as KmpOverviewClassState
import dev.forcetower.melon.feature.overview.domain.model.OverviewMessagesTile as KmpOverviewMessagesTile
import dev.forcetower.melon.feature.overview.domain.model.OverviewNowClass as KmpOverviewNowClass
import dev.forcetower.melon.feature.overview.domain.model.OverviewTodayItem as KmpOverviewTodayItem

// "Hoje" tab — 2026 redesign (dc project `UNES Home - Android`): app bar,
// mesh hero card (next / live / day-done), "Reta final" countdown, the
// "Seu dia" timeline and the latest-message preview. Driven by
// `OverviewViewModel`; this file maps raw KMP payloads into the UI projection
// types declared in `OverviewFixtures.kt`.
@Composable
internal fun OverviewScreen(
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    onOpenDiscipline: (OverviewDiscipline) -> Unit = {},
    onOpenMessages: () -> Unit = {},
    onOpenSchedule: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
) {
    val vm: OverviewViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    val hero = deriveHeroState(state)
    val finalStretch = deriveFinalStretch(state)
    val today = state.todayRaw.map(::mapTodayItem)
    val messages = state.messagesTileRaw?.let { mapMessages(it, state.clock) }

    val name = state.firstName ?: stringResource(R.string.overview_default_user)
    val greeting = stringResource(
        R.string.overview_greeting_format,
        stringResource(
            when (state.greetingKind) {
                GreetingKind.Morning -> R.string.overview_greeting_morning
                GreetingKind.Afternoon -> R.string.overview_greeting_afternoon
                GreetingKind.Evening -> R.string.overview_greeting_evening
            },
        ),
        name,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        OverviewHeader(
            dateEyebrow = state.dateEyebrow,
            greeting = greeting,
            courseLine = state.courseName,
            avatarInitials = state.avatarInitials ?: "?",
            showNotificationDot = (state.messagesTileRaw?.unreadCount ?: 0) > 0,
            onOpenNotifications = onOpenMessages,
            onOpenProfile = onOpenProfile,
            modifier = Modifier.fadeInOnAppear(delayMs = 50),
        )
        Spacer(Modifier.height(18.dp))

        if (hero != null) {
            HeroCard(
                state = hero,
                firstName = name,
                tomorrowEyebrow = state.tomorrowEyebrow,
                isEvening = state.greetingKind == GreetingKind.Evening,
                onOpenClassDetails = { klass ->
                    onOpenDiscipline(
                        OverviewDiscipline(
                            code = klass.code,
                            title = klass.title,
                            offerId = klass.offerId,
                        ),
                    )
                },
                modifier = Modifier.scaleInOnAppear(delayMs = 140, fromScale = 0.97f),
            )
            Spacer(Modifier.height(26.dp))
        }

        if (finalStretch != null) {
            FinalStretchCard(
                data = finalStretch,
                modifier = Modifier.fadeUpOnAppear(delayMs = 550),
            )
            Spacer(Modifier.height(24.dp))
        }

        if (today.isNotEmpty()) {
            TodayTimeline(
                items = today,
                weekdayLabel = state.weekdayLabel,
                onOpenClass = { item ->
                    onOpenDiscipline(
                        OverviewDiscipline(
                            code = item.code,
                            title = item.title,
                            offerId = item.offerId,
                        ),
                    )
                },
                onOpenSchedule = onOpenSchedule,
                modifier = Modifier.fadeUpOnAppear(delayMs = 640),
            )
            Spacer(Modifier.height(22.dp))
        }

        if (messages != null) {
            MessagesPreview(
                data = messages,
                onOpenMessages = onOpenMessages,
                modifier = Modifier.fadeUpOnAppear(delayMs = 920),
            )
        }

        Spacer(Modifier.height(28.dp + bottomInset))
    }
}

// ───────── KMP → UI projection mappers ─────────

// Hero precedence: a class running right now wins; a fully concluded day wins
// over the (tomorrow's) next class; otherwise the next class card; nothing
// renders while there's no schedule at all.
private fun deriveHeroState(state: OverviewUiState): OverviewHeroState? {
    val now = state.nowRaw
    if (now != null && now.isHappeningNow) {
        val start = parseHhMm(now.startTime)
        val end = now.endTime?.let(::parseHhMm)
        val elapsed = (-now.startsInMinutes).coerceAtLeast(0)
        val duration = if (start != null && end != null && end > start) end - start else null
        return OverviewHeroState.Live(
            klass = mapHeroClass(now),
            endsInMinutes = duration?.let { (it - elapsed).coerceAtLeast(0) } ?: 0,
            progress = duration?.let { elapsed.toFloat() / it } ?: 0f,
        )
    }
    val today = state.todayRaw
    if (today.isNotEmpty() && today.all { it.state == KmpOverviewClassState.DONE }) {
        return OverviewHeroState.DayDone(
            classCount = today.size,
            tomorrow = state.tomorrowRaw?.let { raw ->
                OverviewTomorrowUi(
                    title = raw.title,
                    startTime = raw.startTime.take(5),
                    room = raw.roomLocation,
                    extraCount = raw.extraCount,
                )
            },
        )
    }
    if (now != null) {
        return OverviewHeroState.Upcoming(
            klass = mapHeroClass(now),
            startsInMinutes = now.startsInMinutes,
        )
    }
    return null
}

private fun mapHeroClass(raw: KmpOverviewNowClass): OverviewHeroClass = OverviewHeroClass(
    offerId = raw.offerId,
    code = raw.code,
    title = raw.title,
    prof = raw.teacherName?.takeIf { it.isNotBlank() },
    room = raw.roomLocation?.takeIf { it.isNotBlank() },
    timeRange = formatTimeRange(raw.startTime, raw.endTime),
)

// The exam variant wins whenever an evaluation is on the horizon; otherwise
// fall back to the semester-end countdown.
private fun deriveFinalStretch(state: OverviewUiState): OverviewFinalStretch? {
    val exam = state.nextTestTileRaw
    if (exam != null) {
        return OverviewFinalStretch.Exam(
            label = exam.label,
            disciplineName = exam.disciplineName,
            daysUntil = exam.daysUntil,
            dateLabel = formatExamDate(exam.date),
        )
    }
    val daysLeft = state.semesterDaysLeft ?: return null
    val semesterLabel = state.semesterCode ?: return null
    return OverviewFinalStretch.Semester(daysLeft = daysLeft, semesterLabel = semesterLabel)
}

private fun mapTodayItem(raw: KmpOverviewTodayItem): OverviewTodayItem = OverviewTodayItem(
    offerId = raw.offerId,
    code = raw.code,
    title = raw.title,
    startTime = raw.startTime,
    endTime = raw.endTime,
    room = raw.roomLocation?.takeIf { it.isNotBlank() },
    state = when (raw.state) {
        KmpOverviewClassState.DONE -> OverviewClassState.Done
        KmpOverviewClassState.NOW -> OverviewClassState.Now
        KmpOverviewClassState.NEXT -> OverviewClassState.Next
        KmpOverviewClassState.LATER -> OverviewClassState.Later
    },
)

@Composable
private fun mapMessages(
    raw: KmpOverviewMessagesTile,
    clock: Instant,
): OverviewMessagePreview? {
    val sender = raw.lastSender ?: return null
    return OverviewMessagePreview(
        unreadCount = raw.unreadCount,
        sender = sender,
        preview = raw.lastPreview.orEmpty(),
        timeLabel = raw.lastTimestamp?.let { relativeTimeLabel(it, clock) },
    )
}

@Composable
private fun relativeTimeLabel(iso: String, clock: Instant): String? {
    val parsed = runCatching { Instant.parse(iso) }.getOrNull() ?: return null
    val minutes = ((clock.epochSeconds - parsed.epochSeconds) / 60).coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.overview_time_now)
        minutes < 60 -> stringResource(R.string.overview_time_min_short, minutes.toInt())
        minutes < 24 * 60 -> stringResource(R.string.overview_time_hour_short, (minutes / 60).toInt())
        else -> stringResource(R.string.overview_time_day_short, (minutes / (24 * 60)).toInt())
    }
}

// ───────── formatting helpers ─────────

private fun parseHhMm(value: String?): Int? {
    if (value == null) return null
    val parts = value.take(5).split(':')
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return h * 60 + m
}

private fun formatTimeRange(start: String, end: String?): String {
    val s = start.take(5)
    val e = end?.take(5)?.takeIf { it.isNotEmpty() } ?: return s
    return "$s – $e"
}

// "Seg, 21 mar" — device-locale formatted, abbreviation dots stripped,
// leading capital to match the design.
private fun formatExamDate(iso: String): String = runCatching {
    DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
        .format(LocalDate.parse(iso.take(10)))
        .replace(".", "")
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
}.getOrDefault(iso)
