package dev.forcetower.melon.feature.disciplines

import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.query.DisciplineDetailEnrollmentRow
import dev.forcetower.melon.core.database.query.EnrolledDisciplineRow
import dev.forcetower.melon.feature.disciplines.domain.usecase.buildDisciplineDetail
import dev.forcetower.melon.feature.disciplines.domain.usecase.buildDisciplinesListState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// SAGRES fails a discipline below 75% attendance. The allowance is the most
// hours a student can miss and still pass, so 25% of 30h (7,5h) must floor to
// 7: missing 8 leaves 73,3%.
internal class AttendanceAllowanceTest {
    private val semester = SemesterEntity(
        id = "s1",
        platformId = 0,
        code = "20262",
        description = "20262",
        startDate = "2026-08-01",
        endDate = "2026-12-20",
        track = null,
    )

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

    private fun listAllowance(hours: Int): Int {
        val row = EnrolledDisciplineRow(
            studentClassId = "sc1",
            classId = "c1",
            classType = "Teórica",
            groupName = "T01",
            offerId = "o1",
            semesterId = semester.id,
            disciplineHours = hours,
            disciplineId = "d1",
            disciplineCode = "EXA101",
            disciplineName = "Disciplina de Exemplo",
            department = null,
            finalGrade = null,
            approved = null,
            wentToFinals = false,
            missedClasses = 0,
            teacherName = null,
        )
        val state = buildDisciplinesListState(listOf(semester), listOf(row), emptyList(), today = "2026-10-02")
        return state.current!!.disciplines.single().allowedMissedHours
    }

    private fun detailAllowance(hours: Int): Int {
        val row = DisciplineDetailEnrollmentRow(
            offerId = "o1",
            semesterId = semester.id,
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
