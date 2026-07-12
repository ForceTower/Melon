package dev.forcetower.unes.ui.feature.campusevent

import android.content.Context
import android.text.format.DateUtils
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Formatter
import java.util.Locale
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

// Locale-aware display strings for campus events, the Android analogue of
// iOS `CampusEventFormat`. Every date renders in the event's time zone so
// the schedule reads the same for a student traveling outside the campus
// zone.
internal object CampusEventFormat {

    fun zoneId(identifier: String?): ZoneId =
        identifier?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()

    // "Seg" / "Mon" — abbreviated weekday, capitalized, no trailing dot.
    fun weekdayShort(instant: Instant, zone: ZoneId): String =
        format(instant, zone, "EEE")
            .replace(".", "")
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }

    fun weekdayShort(date: LocalDate): String =
        DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
            .format(date.toJava())
            .replace(".", "")
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }

    // "Segunda-feira" / "Monday" — the schedule day heading.
    fun weekdayLong(date: LocalDate): String =
        DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
            .format(date.toJava())
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }

    // "04" — the day-tab number.
    fun dayNumber(date: LocalDate): String = "%02d".format(date.day)

    // "6 de agosto" / "August 6".
    fun fullDate(instant: Instant, zone: ZoneId): String =
        format(instant, zone, bestPattern("dMMMM"))

    // "4 – 8 de agosto" / "August 4 – 8"; `withYear` adds it for the welcome
    // footer. Locale-composed by `DateUtils`, rendered in the event zone.
    fun dateRange(
        context: Context,
        start: Instant,
        end: Instant,
        zone: ZoneId,
        withYear: Boolean = false,
    ): String {
        val flags = DateUtils.FORMAT_SHOW_DATE or
            if (withYear) DateUtils.FORMAT_SHOW_YEAR else DateUtils.FORMAT_NO_YEAR
        return DateUtils.formatDateRange(
            context,
            Formatter(StringBuilder(), Locale.getDefault()),
            start.toEpochMilliseconds(),
            end.toEpochMilliseconds(),
            flags,
            zone.id,
        ).toString()
    }

    // "08:00" / "8:00 AM" — locale-preferred hour cycle.
    fun time(instant: Instant, zone: ZoneId): String =
        format(instant, zone, bestPattern("jm"))

    // "08:00 – 09:30", or just the start for open-ended activities.
    fun timeRange(start: Instant, end: Instant?, zone: ZoneId): String {
        if (end == null) return time(start, zone)
        return "${time(start, zone)} – ${time(end, zone)}"
    }

    data class Countdown(val days: Int, val hours: Int, val minutes: Int, val seconds: Int)

    fun countdown(target: Instant, now: Instant): Countdown {
        val left = (target - now).inWholeSeconds.coerceAtLeast(0).toInt()
        return Countdown(
            days = left / 86_400,
            hours = left % 86_400 / 3600,
            minutes = left % 3600 / 60,
            seconds = left % 60,
        )
    }

    fun padded(value: Int): String = "%02d".format(value)

    private fun format(instant: Instant, zone: ZoneId, pattern: String): String =
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            .withZone(zone)
            .format(java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()))

    private fun bestPattern(skeleton: String): String =
        android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)

    private fun LocalDate.toJava(): java.time.LocalDate =
        java.time.LocalDate.of(year, month.number, day)
}
