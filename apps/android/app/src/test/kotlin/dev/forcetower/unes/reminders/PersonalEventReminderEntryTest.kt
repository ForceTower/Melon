package dev.forcetower.unes.reminders

import dev.forcetower.melon.feature.calendar.domain.model.PersonalEvent
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEventCategory
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEventReminder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class PersonalEventReminderEntryTest {

    private fun event(
        reminder: PersonalEventReminder = PersonalEventReminder.DayBefore,
        completedAt: Long? = null,
    ) = PersonalEvent(
        id = "task",
        title = "Lista 3",
        start = LocalDate(2026, 10, 20),
        end = null,
        category = PersonalEventCategory.Task,
        discipline = null,
        reminder = reminder,
        notes = "",
        createdAt = 0L,
        completedAt = completedAt,
    )

    @Test
    fun `pending entry fires its lead time before the due date`() {
        assertEquals("2026-10-17", event(reminder = PersonalEventReminder.ThreeDays).toReminderEntry()?.fireDateIso)
    }

    @Test
    fun `completed entry schedules nothing`() {
        assertNull(event(completedAt = 1L).toReminderEntry())
    }

    @Test
    fun `entry without a reminder schedules nothing`() {
        assertNull(event(reminder = PersonalEventReminder.None).toReminderEntry())
    }
}
