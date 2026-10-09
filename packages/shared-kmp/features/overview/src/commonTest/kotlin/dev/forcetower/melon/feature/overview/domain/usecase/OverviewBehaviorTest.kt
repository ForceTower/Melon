package dev.forcetower.melon.feature.overview.domain.usecase

import dev.forcetower.melon.core.database.query.AttendanceSummaryRow
import dev.forcetower.melon.core.database.query.SemesterAllocationRow
import dev.forcetower.melon.feature.overview.domain.model.OverviewClassState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime

internal class OverviewBehaviorTest {
    private val fridayClass = SemesterAllocationRow(
        allocationId = "allocation", classId = "class", offerId = "offer", disciplineId = "discipline",
        disciplineCode = "DEMO101", disciplineName = "Algoritmos de Exemplo", day = 5,
        startTime = "10:00", endTime = "12:00", spaceLocation = null, spaceCampus = null,
        spaceModulo = null, teacherName = null,
    )

    @Test
    fun classStartsAtItsOpeningMinuteAndEndsAtItsClosingMinute() {
        val rows = listOf(fridayClass)
        val before = pickNowClass(rows, emptyList(), LocalDateTime(2026, 10, 2, 9, 59))!!
        assertFalse(before.isHappeningNow)
        assertEquals(1, before.startsInMinutes)
        val start = pickNowClass(rows, emptyList(), LocalDateTime(2026, 10, 2, 10, 0))!!
        assertTrue(start.isHappeningNow)
        val end = buildTodayTimeline(rows, emptyList(), LocalDateTime(2026, 10, 2, 12, 0))
        assertEquals(OverviewClassState.DONE, end.single().state)
    }

    @Test
    fun emptyScheduleDoesNotInventAClass() {
        assertEquals(null, pickNowClass(emptyList(), emptyList(), LocalDateTime(2026, 10, 2, 10, 0)))
        assertTrue(buildTodayTimeline(emptyList(), emptyList(), LocalDateTime(2026, 10, 2, 10, 0)).isEmpty())
    }

    @Test
    fun fridayClassStaysUpcomingAcrossSundayBoundary() {
        val upcoming = pickNowClass(listOf(fridayClass), emptyList(), LocalDateTime(2026, 10, 4, 10, 0))!!
        assertEquals(5 * 24 * 60, upcoming.startsInMinutes)
        assertFalse(upcoming.isHappeningNow)
    }

    @Test
    fun attendanceUsesCourseHoursAndClampsBadInputs() {
        assertEquals(97, buildAttendanceTile(AttendanceSummaryRow(2, 60), emptyList()).percentage)
        assertEquals(15, buildAttendanceTile(AttendanceSummaryRow(2, 60), emptyList()).allowedAbsences)
        assertEquals(7, buildAttendanceTile(AttendanceSummaryRow(0, 30), emptyList()).allowedAbsences)
        assertEquals(0, buildAttendanceTile(AttendanceSummaryRow(90, 60), emptyList()).percentage)
        assertEquals(null, buildAttendanceTile(AttendanceSummaryRow(0, 0), emptyList()).percentage)
    }
}
