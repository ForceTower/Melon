package dev.forcetower.unes.ui.feature.disciplinedetail

import kotlin.test.Test
import kotlin.test.assertEquals

// The final exam closes at 0.6 × mean + 0.4 × exam ≥ 5, with the mean
// truncated to a tenth first and the requirement rounded up afterwards.
internal class DisciplineFinalsMathTest {
    @Test
    fun theMeanTruncatesBeforeTheFormula() {
        // 6,95 counts as 6,9: (5 − 4,14) / 0,4 = 2,15 → 2,2.
        assertEquals(2.2, DisciplineFinalsMath.neededFinalGrade(6.95))
    }

    @Test
    fun anExactRequirementIsNotLiftedByFloatNoise() {
        // (5 − 0,6·5,2) / 0,4 is exactly 4,7.
        assertEquals(4.7, DisciplineFinalsMath.neededFinalGrade(5.2))
        assertEquals(8.0, DisciplineFinalsMath.neededFinalGrade(3.0))
    }

    @Test
    fun theRequirementStaysOnTheGradeScale() {
        assertEquals(10.0, DisciplineFinalsMath.neededFinalGrade(1.0))
        assertEquals(0.0, DisciplineFinalsMath.neededFinalGrade(9.0))
    }
}
