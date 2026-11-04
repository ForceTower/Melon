package dev.forcetower.unes.ui.feature.disciplinedetail

import dev.forcetower.unes.ui.feature.calendar.CalendarEvent
import dev.forcetower.unes.ui.feature.calendar.CalendarMath
import dev.forcetower.unes.ui.feature.calendar.PersonalCategory
import dev.forcetower.unes.ui.feature.calendar.PersonalEntry
import dev.forcetower.unes.ui.feature.calendar.PersonalReminder
import dev.forcetower.unes.ui.feature.calendar.asCalendarEvent
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskDueTest {

    private val today = LocalDate.of(2026, 10, 2)

    private fun task(
        id: String = "task",
        start: LocalDate,
        end: LocalDate? = null,
        completedAt: Long? = null,
    ) = PersonalEntry(
        id = id,
        title = id,
        start = start,
        end = end,
        category = PersonalCategory.Task,
        discipline = null,
        reminder = PersonalReminder.DayBefore,
        notes = "",
        createdAt = 0L,
        completedAt = completedAt,
    )

    @Test
    fun `due label counts days to the start`() {
        assertEquals(TaskDue.Today, taskDue(task(start = today), today))
        assertEquals(TaskDue.Tomorrow, taskDue(task(start = today.plusDays(1)), today))
        assertEquals(TaskDue.InDays(7), taskDue(task(start = today.plusDays(7)), today))
        assertEquals(TaskDue.On(today.plusDays(8)), taskDue(task(start = today.plusDays(8)), today))
    }

    @Test
    fun `past due date is overdue unless its range is still running`() {
        assertEquals(TaskDue.Overdue(3), taskDue(task(start = today.minusDays(3)), today))
        assertEquals(TaskDue.Today, taskDue(task(start = today.minusDays(3), end = today.plusDays(1)), today))
        assertEquals(TaskDue.Overdue(1), taskDue(task(start = today.minusDays(3), end = today.minusDays(1)), today))
    }

    @Test
    fun `completed entry reads as done whatever its date`() {
        assertEquals(TaskDue.Done, taskDue(task(start = today.minusDays(3), completedAt = 1L), today))
    }

    @Test
    fun `hero skips completed entries`() {
        val done = task(id = "done", start = today.plusDays(1), completedAt = 1L).asCalendarEvent()
        val next = task(id = "next", start = today.plusDays(4)).asCalendarEvent()
        assertEquals<CalendarEvent?>(next, CalendarMath.nextDeadline(listOf(done, next), today))
    }
}
