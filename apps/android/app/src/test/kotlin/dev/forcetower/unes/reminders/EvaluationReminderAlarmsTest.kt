package dev.forcetower.unes.reminders

import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EvaluationReminderAlarmsTest {

    private fun epochMs(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month - 1, day, hour, minute, 0)
        return cal.timeInMillis
    }

    private fun entry(dateIso: String) = EvaluationReminderSnapshot.Entry(
        key = "disc/plat",
        label = "P2",
        disciplineName = "Cálculo II",
        dateIso = dateIso,
    )

    @Test
    fun `fires at twenty on the eve`() {
        assertEquals(epochMs(2026, 7, 20, 20), EvaluationReminderAlarms.fireEpochMs("2026-07-21"))
    }

    @Test
    fun `eve crosses month boundaries`() {
        assertEquals(epochMs(2026, 7, 31, 20), EvaluationReminderAlarms.fireEpochMs("2026-08-01"))
    }

    @Test
    fun `unparsable dates produce no fire time`() {
        assertNull(EvaluationReminderAlarms.fireEpochMs("not-a-date"))
    }

    @Test
    fun `next fire skips past moments and picks the soonest future one`() {
        val snapshot = EvaluationReminderSnapshot(
            reminders = listOf(entry("2026-07-19"), entry("2026-07-21"), entry("2026-07-25")),
        )
        val now = epochMs(2026, 7, 19, 9)
        assertEquals(epochMs(2026, 7, 20, 20), EvaluationReminderAlarms.nextFireEpochMs(snapshot, now))
    }

    @Test
    fun `next fire is null when everything already passed`() {
        val snapshot = EvaluationReminderSnapshot(reminders = listOf(entry("2026-07-18")))
        assertNull(EvaluationReminderAlarms.nextFireEpochMs(snapshot, epochMs(2026, 7, 19, 9)))
    }

    @Test
    fun `due entries are exactly tomorrow's evaluations`() {
        val snapshot = EvaluationReminderSnapshot(
            reminders = listOf(entry("2026-07-20"), entry("2026-07-21")),
        )
        val firing = epochMs(2026, 7, 19, 20)
        assertEquals(listOf(entry("2026-07-20")), EvaluationReminderAlarms.dueEntries(snapshot, firing))
    }

    @Test
    fun `a doze-deferred alarm that slips past midnight stays silent`() {
        val snapshot = EvaluationReminderSnapshot(reminders = listOf(entry("2026-07-20")))
        val lateFiring = epochMs(2026, 7, 20, 0, 30)
        assertEquals(emptyList(), EvaluationReminderAlarms.dueEntries(snapshot, lateFiring))
    }
}
