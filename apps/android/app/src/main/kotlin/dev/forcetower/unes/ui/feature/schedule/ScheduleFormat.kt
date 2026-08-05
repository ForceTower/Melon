package dev.forcetower.unes.ui.feature.schedule

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonPaletteColors
import dev.forcetower.unes.ui.feature.overview.ColorFor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleClass as KmpScheduleClass
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleDay as KmpScheduleDay
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleWeek as KmpScheduleWeek

// KMP → UI projection and date formatting shared by the two Horário
// renderings (`ScheduleScreen` timeline and `ScheduleGridScreen`). Mirrors
// iOS `apps/ios/UNESKit/Sources/UNESKit/Features/Schedule/ScheduleFormat.swift`.

internal fun mapWeek(
    raw: KmpScheduleWeek?,
    palette: MelonPaletteColors,
): List<List<ScheduleClass>> {
    val days = raw?.days
    if (days.isNullOrEmpty()) return List(7) { emptyList() }
    val bucket = MutableList(7) { emptyList<ScheduleClass>() }
    for (day in days) {
        val idx = day.dayIndex
        if (idx in 0..6) bucket[idx] = mapDay(day, palette)
    }
    return bucket
}

private fun mapDay(day: KmpScheduleDay, palette: MelonPaletteColors): List<ScheduleClass> =
    day.classes.map { mapClass(it, palette) }

private fun mapClass(raw: KmpScheduleClass, palette: MelonPaletteColors): ScheduleClass =
    ScheduleClass(
        start = trimTime(raw.startTime),
        end = raw.endTime?.let(::trimTime).orEmpty(),
        code = raw.code,
        title = raw.title,
        prof = raw.teacherName.orEmpty(),
        color = ColorFor.discipline(palette, raw.code),
        modulo = raw.modulo,
        room = raw.room,
        campus = raw.campus,
        topic = raw.topic,
        offerId = raw.offerId,
        disciplineId = raw.disciplineId,
    )

// Upstream ships HH:mm or HH:mm:ss — trim to five chars so the time rail
// renders minutes only, matching iOS `ScheduleFocusedViewModel.trimTime`.
private fun trimTime(value: String): String = value.take(5)

// "Terça-feira" — full weekday from the device locale, title-cased. Same
// derivation Overview uses (no manual weekday-string surgery).
internal fun formatDayName(iso: String?): String {
    if (iso == null) return ""
    val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return ""
    return DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
        .format(date)
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
}

@Composable
internal fun formatDayDate(iso: String?): String {
    if (iso == null) return ""
    val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return ""
    return stringResource(
        R.string.schedule_day_date_format,
        date.dayOfMonth,
        DateTimeFormatter.ofPattern("MMMM", LocalConfiguration.current.locales[0]).format(date),
    )
}

// "Quinta" — the weekday without the pt-BR "-feira" tail, for the grid's
// agenda section headers and sheet caption. Locales without the tail pass
// through unchanged.
internal fun formatShortDayName(iso: String?): String =
    formatDayName(iso).substringBefore("-feira")

// "17 abr" — the compact date the grid's agenda list pairs with each weekday.
internal fun formatShortDayMonth(iso: String?): String {
    if (iso == null) return ""
    val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return ""
    return "${date.dayOfMonth} ${formatShortMonth(date)}"
}

@Composable
internal fun formatWeekRange(firstIso: String?, lastIso: String?): String {
    if (firstIso == null || lastIso == null) return ""
    val first = runCatching { LocalDate.parse(firstIso) }.getOrNull() ?: return ""
    val last = runCatching { LocalDate.parse(lastIso) }.getOrNull() ?: return ""
    val firstMonth = formatShortMonth(first)
    val lastMonth = formatShortMonth(last)
    return if (first.monthValue == last.monthValue && first.year == last.year) {
        stringResource(
            R.string.schedule_week_range_same_month_format,
            first.dayOfMonth,
            last.dayOfMonth,
            firstMonth,
        )
    } else {
        stringResource(
            R.string.schedule_week_range_spanning_format,
            first.dayOfMonth,
            firstMonth,
            last.dayOfMonth,
            lastMonth,
        )
    }
}

// Mirrors `OverviewScreen.formatShortDate` post-processing — strip the
// trailing dot some locales emit for `MMM`. Rendered uppercase by the header
// anyway.
private fun formatShortMonth(date: LocalDate): String =
    DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
        .format(date)
        .replace(".", "")
