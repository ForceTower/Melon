package dev.forcetower.unes.ui.feature.calendar

import androidx.annotation.StringRes
import dev.forcetower.unes.R
import kotlinx.datetime.number
import java.time.LocalDate
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEvent as KmpPersonalEvent
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEventCategory as KmpCategory
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEventDiscipline as KmpDiscipline
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEventReminder as KmpReminder

// The four kinds the composer offers. `Exam` shares the institutional exam
// tone on purpose — a student-added test reads like any other.
internal enum class PersonalCategory(@StringRes val labelRes: Int, val category: CalendarCategory) {
    Task(R.string.calendar_personal_category_task, CalendarCategory.Task),
    Exam(R.string.calendar_personal_category_exam, CalendarCategory.Exam),
    Study(R.string.calendar_personal_category_study, CalendarCategory.Study),
    Life(R.string.calendar_personal_category_life, CalendarCategory.Life),
}

// Days before the start date; `None` schedules nothing. `shortLabelRes` is the
// segmented-control copy, `labelRes` the detail-sheet one.
internal enum class PersonalReminder(
    val days: Int,
    @StringRes val shortLabelRes: Int,
    @StringRes val labelRes: Int,
) {
    None(0, R.string.calendar_personal_reminder_none, R.string.calendar_personal_reminder_none),
    DayBefore(1, R.string.calendar_personal_reminder_day, R.string.calendar_personal_reminder_day_long),
    ThreeDays(3, R.string.calendar_personal_reminder_three_days, R.string.calendar_personal_reminder_three_days_long),
    Week(7, R.string.calendar_personal_reminder_week, R.string.calendar_personal_reminder_week_long),
}

internal data class PersonalDiscipline(
    val id: String,
    val code: String,
    val name: String,
)

// The student's own entry, as the calendar renders it.
internal data class PersonalEntry(
    val id: String,
    val title: String,
    val start: LocalDate,
    val end: LocalDate?,
    val category: PersonalCategory,
    val discipline: PersonalDiscipline?,
    val reminder: PersonalReminder,
    val notes: String,
    val createdAt: Long,
)

// One entry of the composer's class picker. The tint comes from
// `ColorFor.discipline(code)` at draw time, so a class keeps the same color it
// has on Hoje and Turmas.
internal data class PersonalDisciplineOption(
    val discipline: PersonalDiscipline,
)

// Personal entries join the same timeline. They carry no scope of their own —
// the scope filter routes them through its "Meus" segment.
internal fun PersonalEntry.asCalendarEvent(): CalendarEvent = CalendarEvent(
    id = id,
    description = title,
    start = start,
    end = end,
    fixed = false,
    closed = false,
    scope = CalendarScope.General,
    origin = CalendarOrigin.Manual,
    personal = this,
)

internal fun mapPersonalEvent(event: KmpPersonalEvent): PersonalEntry = PersonalEntry(
    id = event.id,
    title = event.title,
    start = LocalDate.of(event.start.year, event.start.month.number, event.start.day),
    end = event.end?.let { LocalDate.of(it.year, it.month.number, it.day) },
    category = when (event.category) {
        KmpCategory.Task -> PersonalCategory.Task
        KmpCategory.Exam -> PersonalCategory.Exam
        KmpCategory.Study -> PersonalCategory.Study
        KmpCategory.Life -> PersonalCategory.Life
    },
    discipline = event.discipline?.let { PersonalDiscipline(it.id, it.code, it.name) },
    reminder = PersonalReminder.entries.firstOrNull { it.days == event.reminder.days } ?: PersonalReminder.None,
    notes = event.notes,
    createdAt = event.createdAt,
)

internal fun PersonalEntry.toKmp(): KmpPersonalEvent = KmpPersonalEvent(
    id = id,
    title = title,
    start = kotlinx.datetime.LocalDate(start.year, start.monthValue, start.dayOfMonth),
    end = end?.let { kotlinx.datetime.LocalDate(it.year, it.monthValue, it.dayOfMonth) },
    category = when (category) {
        PersonalCategory.Task -> KmpCategory.Task
        PersonalCategory.Exam -> KmpCategory.Exam
        PersonalCategory.Study -> KmpCategory.Study
        PersonalCategory.Life -> KmpCategory.Life
    },
    discipline = discipline?.let { KmpDiscipline(it.id, it.code, it.name) },
    reminder = KmpReminder.fromDays(reminder.days),
    notes = notes,
    createdAt = createdAt,
)
