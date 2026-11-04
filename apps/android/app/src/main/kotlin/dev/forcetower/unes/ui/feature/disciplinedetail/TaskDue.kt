package dev.forcetower.unes.ui.feature.disciplinedetail

import dev.forcetower.unes.ui.feature.calendar.PersonalEntry
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Where a checklist entry stands against its due date, which is the entry's
// start. A range that is still running is due today rather than overdue.
internal sealed interface TaskDue {
    data object Done : TaskDue

    data object Today : TaskDue

    data object Tomorrow : TaskDue

    data class InDays(
        val days: Int,
    ) : TaskDue

    data class On(
        val date: LocalDate,
    ) : TaskDue

    data class Overdue(
        val days: Int,
    ) : TaskDue
}

internal fun taskDue(
    entry: PersonalEntry,
    today: LocalDate,
): TaskDue {
    if (entry.isCompleted) return TaskDue.Done
    val toStart = ChronoUnit.DAYS.between(today, entry.start).toInt()
    val toEnd = ChronoUnit.DAYS.between(today, entry.end ?: entry.start).toInt()
    return when {
        toEnd < 0 -> TaskDue.Overdue(-toEnd)
        toStart <= 0 -> TaskDue.Today
        toStart == 1 -> TaskDue.Tomorrow
        toStart <= 7 -> TaskDue.InDays(toStart)
        else -> TaskDue.On(entry.start)
    }
}
