package dev.forcetower.unes.ui.feature.calendar

import androidx.annotation.StringRes
import dev.forcetower.unes.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Audience the event reaches. Mirrors the upstream `scope` field returned by
// the academic-calendar feed (KMP `CalendarFeedScope`).
internal enum class CalendarScope(@StringRes val labelRes: Int) {
    General(R.string.calendar_scope_general),
    Faculty(R.string.calendar_scope_faculty),
    Course(R.string.calendar_scope_course),
    ClassScope(R.string.calendar_scope_class),
    Campus(R.string.calendar_scope_campus),
}

// How the event ended up in the calendar — combined with `closed` to derive
// the visual category.
internal enum class CalendarOrigin {
    Manual, Evaluation, FinalExam, SecondCall, SecondEpoch,
}

// Visual category derived from `closed` + `origin`.
internal enum class CalendarCategory(@StringRes val labelRes: Int) {
    Holiday(R.string.calendar_category_holiday),
    Exam(R.string.calendar_category_exam),
    Deadline(R.string.calendar_category_deadline),
}

internal enum class CalendarStatus { Past, Active, Future }

internal data class CalendarEvent(
    val id: String,
    val description: String,
    val start: LocalDate,
    // Null for single-day events.
    val end: LocalDate?,
    // Repeats every year — surfaces an "anual" tag on the row.
    val fixed: Boolean,
    // "Closed" days where the campus is shut. Holiday cards render filled.
    val closed: Boolean,
    val scope: CalendarScope,
    val origin: CalendarOrigin,
) {
    val endOrStart: LocalDate get() = end ?: start

    // Drop the `" — Estudante"` suffix the SAGRES feed appends to scope-specific
    // rows — the row already shows the scope in its eyebrow.
    val displayDescription: String
        get() = description.replace(" — Estudante", "")
}

internal sealed interface CountdownToken {
    data object Today : CountdownToken
    data object Tomorrow : CountdownToken
    data class Number(val value: Int) : CountdownToken
}

internal data class CountdownParts(val number: CountdownToken, @StringRes val tailRes: Int? = null)

internal object CalendarMath {
    val today: LocalDate get() = LocalDate.now()

    fun daysBetween(a: LocalDate, b: LocalDate): Int =
        ChronoUnit.DAYS.between(a, b).toInt()

    fun categorize(ev: CalendarEvent): CalendarCategory {
        if (ev.closed) return CalendarCategory.Holiday
        return when (ev.origin) {
            CalendarOrigin.Evaluation,
            CalendarOrigin.FinalExam,
            CalendarOrigin.SecondCall,
            CalendarOrigin.SecondEpoch -> CalendarCategory.Exam
            CalendarOrigin.Manual -> CalendarCategory.Deadline
        }
    }

    fun status(ev: CalendarEvent, today: LocalDate = this.today): CalendarStatus {
        val s = ev.start
        val e = ev.end ?: s
        val ds = daysBetween(today, s)
        val de = daysBetween(today, e)
        return when {
            de < 0 -> CalendarStatus.Past
            ds > 0 -> CalendarStatus.Future
            else -> CalendarStatus.Active
        }
    }

    // Hero countdown — split into a big number and a tail label so the card
    // can typeset them at different sizes. Mirrors iOS `countdownParts`.
    fun countdownParts(ev: CalendarEvent, today: LocalDate = this.today): CountdownParts {
        val s = ev.start
        val e = ev.end ?: s
        val ds = daysBetween(today, s)
        val de = daysBetween(today, e)
        return when {
            ds == 0 -> CountdownParts(CountdownToken.Today)
            ds == 1 -> CountdownParts(CountdownToken.Tomorrow)
            ds == -1 && ev.end != null ->
                CountdownParts(CountdownToken.Number(1), R.string.calendar_countdown_tail_one_day_to_close)
            ds > 0 -> CountdownParts(CountdownToken.Number(ds), R.string.calendar_countdown_tail_days)
            ds < 0 && de >= 0 ->
                CountdownParts(CountdownToken.Number(de), R.string.calendar_countdown_tail_days_to_close)
            else -> CountdownParts(CountdownToken.Number(kotlin.math.abs(ds)), R.string.calendar_countdown_tail_days_ago)
        }
    }

    // Pick the most actionable card for the hero. Active deadlines (closing
    // soonest) win, else the nearest upcoming event.
    fun nextDeadline(events: List<CalendarEvent>, today: LocalDate = this.today): CalendarEvent? {
        val active = events.filter { !it.closed && status(it, today) == CalendarStatus.Active }
        if (active.isNotEmpty()) {
            return active.minByOrNull { it.endOrStart }
        }
        val future = events.filter { status(it, today) == CalendarStatus.Future }
        if (future.isNotEmpty()) {
            return future.minByOrNull { it.start }
        }
        return null
    }
}

internal enum class CalendarCategoryFilter(@StringRes val labelRes: Int) {
    All(R.string.calendar_filter_category_all),
    Deadline(R.string.calendar_filter_category_deadlines),
    Exam(R.string.calendar_filter_category_exams),
    Holiday(R.string.calendar_filter_category_holidays);

    fun matches(ev: CalendarEvent): Boolean = when (this) {
        All -> true
        Deadline -> CalendarMath.categorize(ev) == CalendarCategory.Deadline
        Exam -> CalendarMath.categorize(ev) == CalendarCategory.Exam
        Holiday -> CalendarMath.categorize(ev) == CalendarCategory.Holiday
    }
}

internal enum class CalendarScopeFilter(@StringRes val labelRes: Int) {
    All(R.string.calendar_filter_scope_all),
    General(R.string.calendar_scope_general),
    Faculty(R.string.calendar_scope_faculty),
    Course(R.string.calendar_scope_course),
    ClassScope(R.string.calendar_scope_class);

    fun matches(ev: CalendarEvent): Boolean = when (this) {
        All -> true
        General -> ev.scope == CalendarScope.General
        Faculty -> ev.scope == CalendarScope.Faculty
        Course -> ev.scope == CalendarScope.Course
        ClassScope -> ev.scope == CalendarScope.ClassScope
    }
}

internal data class CalendarMonthGroup(
    val year: Int,
    // 1..12 — matches `LocalDate.monthValue`.
    val month: Int,
    val events: List<CalendarEvent>,
) {
    val id: String get() = "%04d-%02d".format(year, month)
}

internal fun List<CalendarEvent>.groupedByMonth(): List<CalendarMonthGroup> {
    val buckets = LinkedHashMap<String, MutableList<CalendarEvent>>()
    forEach { ev ->
        val key = "%04d-%02d".format(ev.start.year, ev.start.monthValue)
        buckets.getOrPut(key) { mutableListOf() }.add(ev)
    }
    return buckets.entries
        .sortedBy { it.key }
        .map { (key, events) ->
            val parts = key.split('-')
            CalendarMonthGroup(
                year = parts[0].toInt(),
                month = parts[1].toInt(),
                events = events,
            )
        }
}

// pt-BR formatting helpers — mirrors iOS `CalendarFormat` so date strings
// stay identical across platforms.
internal object CalendarFormat {
    val monthsShort: List<String> = listOf(
        "jan", "fev", "mar", "abr", "mai", "jun",
        "jul", "ago", "set", "out", "nov", "dez",
    )

    val monthsLong: List<String> = listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    )

    // Indexed 1..7 with `LocalDate.dayOfWeek.value`: 1=monday … 7=sunday.
    private val weekdaysShort: List<String> = listOf(
        "seg", "ter", "qua", "qui", "sex", "sáb", "dom",
    )

    fun dateShort(d: LocalDate): String = "%02d %s".format(d.dayOfMonth, monthsShort[d.monthValue - 1])

    fun dateRange(start: LocalDate, end: LocalDate?): String {
        if (end == null) return dateShort(start)
        return if (start.monthValue == end.monthValue) {
            "%02d – %02d %s".format(start.dayOfMonth, end.dayOfMonth, monthsShort[start.monthValue - 1])
        } else {
            "${dateShort(start)} – ${dateShort(end)}"
        }
    }

    fun weekday(d: LocalDate): String = weekdaysShort[d.dayOfWeek.value - 1]
}
