package dev.forcetower.melon.feature.disciplines

import dev.forcetower.melon.core.database.query.DisciplineDetailEnrollmentRow
import dev.forcetower.melon.feature.disciplines.DisciplineListFixtures.listItem
import dev.forcetower.melon.feature.disciplines.domain.usecase.buildDisciplineDetail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// SAGRES fails a discipline below 75% attendance. The allowance is the most
// hours a student can miss and still pass, so 25% of 30h (7,5h) must floor to
// 7: missing 8 leaves 73,3%.
internal class AttendanceAllowanceTest {
    @Test
    fun listAndDetailFloorTheQuarterRule() {
        for ((hours, allowed) in listOf(30 to 7, 45 to 11, 60 to 15, 75 to 18, 90 to 22)) {
            assertEquals(allowed, listAllowance(hours), "list, ${hours}h")
            assertEquals(allowed, detailAllowance(hours), "detail, ${hours}h")
        }
    }

    @Test
    fun missingTheWholeAllowanceStillKeepsSeventyFivePercent() {
        for (hours in 1..200) {
            val allowed = listAllowance(hours)
            assertTrue(4 * (hours - allowed) >= 3 * hours, "${hours}h: missing $allowed must keep 75%")
            assertTrue(4 * (hours - allowed - 1) < 3 * hours, "${hours}h: missing ${allowed + 1} must fail")
            assertEquals(allowed, detailAllowance(hours), "detail, ${hours}h")
        }
    }

    private fun listAllowance(hours: Int): Int = listItem(hours).allowedMissedHours

    private fun detailAllowance(hours: Int): Int {
        val row = DisciplineDetailEnrollmentRow(
            offerId = "o1",
            semesterId = "s1",
            disciplineId = "d1",
            disciplineCode = "EXA101",
            disciplineName = "Disciplina de Exemplo",
            disciplineHours = hours,
            disciplineProgram = null,
            department = null,
            offerHours = null,
            studentClassId = "sc1",
            classId = "c1",
            classType = "Teórica",
            groupName = "T01",
            finalGrade = null,
            approved = null,
            wentToFinals = false,
            missedClasses = 0,
            teacherName = null,
        )
        return buildDisciplineDetail(listOf(row), emptyList(), emptyList(), emptyList()).allowedMissedHours
    }
}
