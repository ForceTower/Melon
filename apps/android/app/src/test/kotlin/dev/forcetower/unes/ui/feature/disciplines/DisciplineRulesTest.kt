package dev.forcetower.unes.ui.feature.disciplines

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Same examples as iOS `DisciplinesMappingTests`, so both clients reach the
// same verdicts from the same academic record.
internal class DisciplineRulesTest {
    @Test
    fun absenceRiskWarnsAtHalfTheAllowanceAndTurnsCriticalAtThreeQuarters() {
        assertEquals(AbsenceRisk.Ok, discipline(absences = 3, allowed = 8).absenceRisk)
        assertEquals(AbsenceRisk.Warn, discipline(absences = 4, allowed = 8).absenceRisk)
        assertEquals(AbsenceRisk.Warn, discipline(absences = 5, allowed = 8).absenceRisk)
        assertEquals(AbsenceRisk.Risk, discipline(absences = 6, allowed = 8).absenceRisk)
        assertEquals(AbsenceRisk.Risk, discipline(absences = 9, allowed = 8).absenceRisk)
    }

    @Test
    fun anyAbsenceIsCriticalWhenNothingCanBeMissed() {
        assertEquals(AbsenceRisk.Ok, discipline(absences = 0, allowed = 0).absenceRisk)
        assertEquals(AbsenceRisk.Risk, discipline(absences = 1, allowed = 0).absenceRisk)
    }

    @Test
    fun statusTrustsUpstreamBeforeInferringFromGrades() {
        // A finals passer closes with a 5–7 mean — the flag must win.
        val closed = discipline(storedPartialAverage = 5.2, finalGrade = 5.8)
        assertEquals(DisciplineStatus.Key.Approved, closed.copy(approved = true).status.key)
        assertEquals(DisciplineStatus.Key.Failed, closed.copy(approved = false).status.key)

        assertEquals(DisciplineStatus.Key.Final, discipline(wentToFinals = true, finalGrade = 3.5).status.key)

        assertEquals(DisciplineStatus.Key.Approved, discipline(finalGrade = 8.0).status.key)
        assertEquals(DisciplineStatus.Key.Final, discipline(finalGrade = 6.0).status.key)
        assertEquals(DisciplineStatus.Key.Failed, discipline(finalGrade = 4.5).status.key)

        assertEquals(DisciplineStatus.Key.Pending, discipline().status.key)
        assertEquals(DisciplineStatus.Key.Low, discipline(storedPartialAverage = 5.0).status.key)
        assertEquals(DisciplineStatus.Key.Ongoing, discipline(storedPartialAverage = 7.5).status.key)
    }

    @Test
    fun partialAverageWeighsReleasedGradesAndIgnoresPendingOnes() {
        val weighted = discipline(
            grades = listOf(
                grade(score = 6.8, weight = 2.5),
                grade(score = 9.0, weight = 5.0),
                grade(score = null, weight = 2.5),
            ),
        )
        assertEquals(62.0 / 7.5, weighted.partialAverage!!, 1e-9)

        val missingWeight = discipline(grades = listOf(grade(score = 8.0, weight = 2.0), grade(score = 6.0)))
        assertEquals(7.0, missingWeight.partialAverage!!, 1e-9)

        assertNull(discipline(grades = listOf(grade(score = null))).partialAverage)
        assertEquals(4.2, discipline(storedPartialAverage = 4.2, grades = listOf(grade(score = 9.0))).partialAverage)
    }

    @Test
    fun displayedGradesTruncateInsteadOfRounding() {
        assertEquals("6,9", formatGrade(6.95))
        assertEquals("8,7", formatGrade(8.7))
        assertEquals("10,0", formatGrade(10.0))
        assertEquals("–", formatGrade(null))
    }

    private fun discipline(
        absences: Int = 0,
        allowed: Int = 15,
        grades: List<GradeEntry> = emptyList(),
        storedPartialAverage: Double? = null,
        finalGrade: Double? = null,
        approved: Boolean? = null,
        wentToFinals: Boolean = false,
    ) = Discipline(
        code = "EXA101",
        fullCode = "EXA101",
        title = "Disciplina de Exemplo",
        dept = "",
        prof = "",
        color = Color.Unspecified,
        hours = 60,
        absences = absences,
        allowedAbsences = allowed,
        sections = listOf(GradeSection(name = "Geral", grades = grades)),
        finalGrade = finalGrade,
        approved = approved,
        wentToFinals = wentToFinals,
        storedPartialAverage = storedPartialAverage,
    )

    private fun grade(
        score: Double?,
        weight: Double? = null,
    ) = GradeEntry(label = "AV", title = "Avaliação", date = null, score = score, weight = weight)
}
