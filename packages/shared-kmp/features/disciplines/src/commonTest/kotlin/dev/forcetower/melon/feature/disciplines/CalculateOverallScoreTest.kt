package dev.forcetower.melon.feature.disciplines

import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.query.EnrolledDisciplineRow
import dev.forcetower.melon.feature.disciplines.domain.usecase.computeOverallScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// A student who moves from a graduação into a mestrado keeps taking classes
// under the same login, and the mestrado runs on its own semester calendar
// (a track). Averaging both into one CR produces a number that describes
// neither degree — see issue #55.
class CalculateOverallScoreTest {

    private val undergrad1 = semester("ug1", code = "20241", track = null, start = "2024-03-01")
    private val undergrad2 = semester("ug2", code = "20242", track = null, start = "2024-08-01")
    private val masters1 = semester("ms1", code = "25.1RUE", track = "RUE", start = "2025-03-01")

    @Test
    fun undergrad_only_student_has_a_single_program() {
        val score = computeOverallScore(
            semesters = listOf(undergrad1, undergrad2),
            enrollments = listOf(
                enrollment("o1", undergrad1, grade = "8.0", hours = 60),
                enrollment("o2", undergrad2, grade = "6.0", hours = 60),
            ),
        )

        assertEquals(1, score.programs.size)
        assertTrue(!score.isSplit)
        assertEquals(7.0, score.current?.value)
        assertNull(score.current?.track)
    }

    @Test
    fun masters_disciplines_do_not_move_the_undergrad_cr() {
        val score = computeOverallScore(
            semesters = listOf(undergrad1, undergrad2, masters1),
            enrollments = listOf(
                enrollment("o1", undergrad1, grade = "8.0", hours = 60),
                enrollment("o2", undergrad2, grade = "6.0", hours = 60),
                enrollment("o3", masters1, grade = "10.0", hours = 60),
            ),
        )

        assertTrue(score.isSplit)
        assertEquals(listOf("RUE", null), score.programs.map { it.track })
        assertEquals(7.0, score.programs.first { it.track == null }.value)
        assertEquals(10.0, score.programs.first { it.track == "RUE" }.value)
    }

    @Test
    fun the_current_program_is_the_one_of_the_newest_enrollment() {
        val score = computeOverallScore(
            semesters = listOf(undergrad1, masters1),
            enrollments = listOf(
                enrollment("o1", undergrad1, grade = "8.0", hours = 60),
                enrollment("o2", masters1, grade = "10.0", hours = 60),
            ),
        )

        assertEquals("RUE", score.currentTrack)
        assertEquals(10.0, score.current?.value)
    }

    // A mestrado in its first semester has enrollments but no closed grades,
    // so it has no CR yet. The screens must still fall back to a real number
    // instead of rendering nothing.
    @Test
    fun a_program_without_closed_grades_falls_back_to_the_newest_scored_one() {
        val score = computeOverallScore(
            semesters = listOf(undergrad1, masters1),
            enrollments = listOf(
                enrollment("o1", undergrad1, grade = "8.0", hours = 60),
                enrollment("o2", masters1, grade = null, hours = 60),
            ),
        )

        assertEquals("RUE", score.currentTrack)
        assertEquals(1, score.programs.size)
        assertEquals(8.0, score.current?.value)
        assertNull(score.current?.track)
    }

    // The delta is the program's own movement: the undergrad CR must not
    // jump because a mestrado semester closed in between.
    @Test
    fun delta_compares_against_the_previous_semester_of_the_same_program() {
        val score = computeOverallScore(
            semesters = listOf(undergrad1, masters1, undergrad2),
            enrollments = listOf(
                enrollment("o1", undergrad1, grade = "6.0", hours = 60),
                enrollment("o2", masters1, grade = "10.0", hours = 60),
                enrollment("o3", undergrad2, grade = "8.0", hours = 60),
            ),
        )

        val undergrad = score.programs.first { it.track == null }
        assertEquals(7.0, undergrad.value)
        assertEquals(1.0, undergrad.delta)
        assertNull(score.programs.first { it.track == "RUE" }.delta)
    }

    // Theory + practice groups arrive as two rows for one offer, and the
    // final grade lands on only one of them.
    @Test
    fun multi_group_disciplines_count_once() {
        val score = computeOverallScore(
            semesters = listOf(undergrad1),
            enrollments = listOf(
                enrollment("o1", undergrad1, grade = null, hours = 60, classId = "c1"),
                enrollment("o1", undergrad1, grade = "9.0", hours = 60, classId = "c2"),
                enrollment("o2", undergrad1, grade = "7.0", hours = 60),
            ),
        )

        assertEquals(8.0, score.current?.value)
        assertEquals(2, score.current?.disciplineCount)
    }

    private fun semester(id: String, code: String, track: String?, start: String) = SemesterEntity(
        id = id,
        platformId = 0,
        code = code,
        description = code,
        startDate = start,
        endDate = start,
        track = track,
    )

    private fun enrollment(
        offerId: String,
        semester: SemesterEntity,
        grade: String?,
        hours: Int,
        classId: String = offerId,
    ) = EnrolledDisciplineRow(
        studentClassId = "sc-$classId",
        classId = classId,
        classType = "Teórica",
        groupName = "T01",
        offerId = offerId,
        semesterId = semester.id,
        disciplineHours = hours,
        disciplineId = "d-$offerId",
        disciplineCode = "EXA$offerId",
        disciplineName = "Disciplina $offerId",
        department = null,
        finalGrade = grade,
        approved = grade != null,
        wentToFinals = false,
        missedClasses = null,
        teacherName = null,
    )
}
