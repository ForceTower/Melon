package dev.forcetower.melon.feature.calendar.domain.model

import kotlinx.datetime.LocalDate

// A calendar entry the student created themselves. Purely device-local: there
// is no endpoint behind any of this, and it shares the Calendário timeline with
// the academic feed.
data class PersonalEvent(
    val id: String,
    val title: String,
    val start: LocalDate,
    // Null for single-day entries.
    val end: LocalDate?,
    val category: PersonalEventCategory,
    val discipline: PersonalEventDiscipline?,
    val reminder: PersonalEventReminder,
    val notes: String,
    val createdAt: Long,
)

enum class PersonalEventCategory(val wire: String) {
    Task("TASK"),
    Exam("EXAM"),
    Study("STUDY"),
    Life("LIFE"),
    ;

    companion object {
        // An unknown kind can only come from a newer build's row — the generic
        // tone beats dropping the entry.
        fun fromWire(value: String): PersonalEventCategory =
            entries.firstOrNull { it.wire == value } ?: Task
    }
}

// Days before the start date; `None` schedules nothing.
enum class PersonalEventReminder(val days: Int) {
    None(0),
    DayBefore(1),
    ThreeDays(3),
    Week(7),
    ;

    companion object {
        fun fromDays(days: Int): PersonalEventReminder =
            entries.firstOrNull { it.days == days } ?: None
    }
}

data class PersonalEventDiscipline(
    val id: String,
    val code: String,
    val name: String,
)
