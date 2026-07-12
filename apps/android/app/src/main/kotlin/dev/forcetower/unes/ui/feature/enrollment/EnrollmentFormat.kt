package dev.forcetower.unes.ui.feature.enrollment

import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSection
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil

// Locale-aware display helpers for the matrícula flow. Window dates arrive as
// offset datetimes with optional seconds; unparseable values degrade labels,
// never the flow (mirrors iOS `EnrollmentFormat`).
internal object EnrollmentFormat {

    fun parseDate(raw: String): OffsetDateTime? =
        runCatching { OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME) }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toOffsetDateTime()
            }.getOrNull()

    // Whole days until the deadline, ceiling so "4d 2h left" still reads 5.
    fun daysLeft(end: OffsetDateTime, nowMillis: Long): Int {
        val seconds = Instant.ofEpochMilli(nowMillis).until(end.toInstant(), ChronoUnit.SECONDS)
        return ceil(seconds / 86_400.0).toInt().coerceAtLeast(0)
    }

    // Fraction of the window still remaining — the hero ring fill.
    fun remainingFraction(start: OffsetDateTime, end: OffsetDateTime, nowMillis: Long): Float {
        val total = start.toInstant().until(end.toInstant(), ChronoUnit.SECONDS).toFloat()
        if (total <= 0f) return 0f
        val left = Instant.ofEpochMilli(nowMillis).until(end.toInstant(), ChronoUnit.SECONDS).toFloat()
        return (left / total).coerceIn(0f, 1f)
    }

    // "15 jun" — pt-BR emits "15 jun."; the trailing period is stripped like
    // the Me document sheet does.
    fun shortDate(date: OffsetDateTime): String =
        DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
            .format(date.atZoneSameInstant(ZoneId.systemDefault()))
            .replace(".", "")

    // "23h59" — the design's compact deadline time.
    fun timeLabel(date: OffsetDateTime): String =
        DateTimeFormatter.ofPattern("HH'h'mm", Locale.getDefault())
            .format(date.atZoneSameInstant(ZoneId.systemDefault()))

    // Slot day ints are 0=Sunday…6=Saturday on the wire.
    private fun dayOfWeek(day: Int): DayOfWeek =
        if (day == 0) DayOfWeek.SUNDAY else DayOfWeek.of(day.coerceIn(1, 6))

    // "Seg" — locale short name, period stripped, title-cased.
    fun dayShort(day: Int): String =
        dayOfWeek(day).getDisplayName(TextStyle.SHORT, Locale.getDefault())
            .removeSuffix(".")
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }

    // "segunda" — for conflict copy ("Choca com … na segunda").
    fun dayFull(day: Int): String =
        dayOfWeek(day).getDisplayName(TextStyle.FULL, Locale.getDefault())
            .lowercase(Locale.getDefault())

    // "13:30" — slot times may carry seconds on the wire.
    fun slotTime(raw: String): String = raw.take(5)
}

internal data class ScheduleLine(val days: String, val time: String)

// Collapses a section's slots into per-time lines: "Seg, Qua · 13:30–15:30".
internal fun scheduleLines(section: EnrollmentSection): List<ScheduleLine> {
    val byTime = LinkedHashMap<String, MutableList<Int>>()
    section.allSlots
        .sortedWith(compareBy({ slotMinutes(it.start) }, { it.day }))
        .forEach { slot ->
            val key = "${EnrollmentFormat.slotTime(slot.start)}–${EnrollmentFormat.slotTime(slot.end)}"
            byTime.getOrPut(key) { mutableListOf() }.add(slot.day)
        }
    return byTime.map { (time, days) ->
        ScheduleLine(
            days = days.sorted().distinct().joinToString(", ") { EnrollmentFormat.dayShort(it) },
            time = time,
        )
    }
}
