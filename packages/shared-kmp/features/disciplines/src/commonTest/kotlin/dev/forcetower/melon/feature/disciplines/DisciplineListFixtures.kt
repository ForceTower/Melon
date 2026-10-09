package dev.forcetower.melon.feature.disciplines

import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.query.EnrolledDisciplineRow
import dev.forcetower.melon.core.database.query.PartialGradeRow
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineListItem
import dev.forcetower.melon.feature.disciplines.domain.usecase.buildDisciplinesListState

internal object DisciplineListFixtures {
    val semester = SemesterEntity(
        id = "s1",
        platformId = 0,
        code = "20262",
        description = "20262",
        startDate = "2026-08-01",
        endDate = "2026-12-20",
        track = null,
    )

    fun listItem(
        hours: Int = 60,
        grades: List<PartialGradeRow> = emptyList(),
    ): DisciplineListItem {
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
        val state = buildDisciplinesListState(listOf(semester), listOf(row), grades, today = "2026-10-02")
        return state.current!!.disciplines.single()
    }

    fun grade(
        ordinal: Int,
        name: String,
        nameShort: String?,
        value: String?,
        weight: String = "1.00",
    ) = PartialGradeRow(
        gradeId = "g$ordinal",
        gradePlatformId = "p$ordinal",
        studentClassId = "sc1",
        name = name,
        nameShort = nameShort,
        ordinal = ordinal,
        weight = weight,
        value = value,
        date = null,
    )
}
