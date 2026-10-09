package dev.forcetower.melon.feature.disciplines

import dev.forcetower.melon.feature.disciplines.DisciplineListFixtures.grade
import dev.forcetower.melon.feature.disciplines.DisciplineListFixtures.listItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// The partial mean covers the semester's own evaluations. The final exam closes
// the discipline separately (0.6 × mean + 0.4 × exam), so iOS and the detail
// screen keep it out of the mean; the list card must agree.
internal class ListPartialAverageTest {
    @Test
    fun aPublishedFinalExamStaysOutOfThePartialMean() {
        val item = listItem(
            grades = listOf(
                grade(1, "Avaliação 1", "AV1", "4.00", weight = "2.00"),
                grade(2, "Avaliação 2", "AV2", "6.00"),
                grade(3, "Prova Final", "Adicional", "9.00"),
            ),
        )
        assertEquals(14.0 / 3.0, item.partialAverage!!, 1e-9)
        assertEquals(listOf("AV1", "AV2", "Adicional"), item.grades.map { it.nameShort })
    }

    @Test
    fun aRegularEvaluationSharingTheFinalExamNameStillCounts() {
        val item = listItem(
            grades = listOf(
                grade(1, "Avaliação 1", "AV1", "4.00"),
                grade(2, "Prova Final", "AV2", "8.00"),
            ),
        )
        assertEquals(6.0, item.partialAverage!!, 1e-9)
    }

    @Test
    fun onlyAFinalExamLeavesNoPartialMean() {
        val item = listItem(
            grades = listOf(
                grade(1, "Avaliação 1", "AV1", null),
                grade(2, " prova final ", "ADICIONAL", "7.00"),
            ),
        )
        assertNull(item.partialAverage)
    }
}
