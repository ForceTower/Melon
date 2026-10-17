package dev.forcetower.melon.feature.overview.domain.usecase

import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.query.EnrolledDisciplineRow
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GradeTileTest {
    private val past = semester("s1", start = "2026-02-01", end = "2026-06-30")
    private val active = semester("s2", start = "2026-08-01", end = "2026-12-20")

    // Same rule as the Disciplines CR: a failure posted without a mean weighs
    // a 0 over its hours, and a running class stays out.
    @Test
    fun aFailurePostedWithoutAMeanScoresZero() {
        val tile = buildTile(
            semesters = listOf(past, active),
            enrollments = listOf(
                enrollment("o1", past, grade = "8.0", approved = true, hours = 60),
                enrollment("o2", past, grade = null, approved = false, hours = 30),
                enrollment("o3", active, grade = null, approved = null, hours = 60),
            ),
            todayIso = "2026-10-02",
        )

        assertEquals(480.0 / 90, tile?.cr)
    }

    private fun semester(
        id: String,
        start: String,
        end: String,
    ) = SemesterEntity(
        id = id,
        platformId = 0,
        code = id,
        description = id,
        startDate = start,
        endDate = end,
        track = null,
    )

    private fun enrollment(
        offerId: String,
        semester: SemesterEntity,
        grade: String?,
        approved: Boolean?,
        hours: Int,
    ) = EnrolledDisciplineRow(
        studentClassId = "sc-$offerId",
        classId = "c-$offerId",
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
        approved = approved,
        wentToFinals = false,
        missedClasses = null,
        teacherName = null,
    )
}
