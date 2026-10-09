package dev.forcetower.unes.ui.feature.finalcountdown

import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineListItem
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineStatusKind
import dev.forcetower.melon.feature.disciplines.domain.model.ListGradeEntry
import kotlin.test.Test
import kotlin.test.assertEquals

internal class FinalCountdownSeedTest {
    // Teachers also name regular evaluations "Prova Final"; only the row that
    // also carries the "Adicional" short label is the final exam, as on iOS.
    @Test
    fun onlyTheFinalExamRowIsLeftOutOfTheCalculator() {
        val choice = mapChoice(
            listItem(
                grade("Avaliação 1", "AV1", 7.0),
                grade("Prova Final", "AV2", 8.0),
                grade("Prova Final", "Adicional", null),
            ),
            semesterLabel = "2026.2",
        )
        assertEquals(listOf("AV1", "AV2"), choice.seedGrades.map { it.label })
    }

    private fun listItem(vararg grades: ListGradeEntry) =
        DisciplineListItem(
            disciplineId = "d1",
            offerId = "o1",
            semesterId = "s1",
            studentClassIds = listOf("sc1"),
            code = "EXA101",
            name = "Disciplina de Exemplo",
            department = null,
            teacherName = null,
            hours = 60,
            missedHours = 0,
            allowedMissedHours = 15,
            partialAverage = null,
            finalGrade = null,
            approved = null,
            wentToFinals = false,
            status = DisciplineStatusKind.PENDING,
            groupsLabel = null,
            grades = grades.toList(),
        )

    private fun grade(
        name: String,
        nameShort: String,
        value: Double?,
    ) = ListGradeEntry(name = name, nameShort = nameShort, date = null, value = value, weight = 1.0)
}
