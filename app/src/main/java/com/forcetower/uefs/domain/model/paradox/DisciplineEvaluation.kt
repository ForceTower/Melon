package com.forcetower.uefs.domain.model.paradox

import com.forcetower.uefs.core.model.edge.paradox.PublicDisciplineEvaluationCombinedData
import com.forcetower.uefs.core.model.edge.paradox.PublicDisciplineEvaluationData
import java.time.ZonedDateTime

data class DisciplineCombinedData(
    val ref: PublicDisciplineEvaluationCombinedData,
    val semesters: List<SemesterMean>,
    val teachers: List<TeacherMean> = emptyList()
)

data class SemesterMean(
    val id: Long,
    val name: String,
    val mean: Double,
    val start: ZonedDateTime,
    val studentCountWeighted: Int
)

data class TeacherMean(
    val id: String,
    val name: String,
    val lastSeen: String,
    val mean: Double,
    val studentCount: Int,
    val studentCountWeighted: Int,
    val semesterStart: ZonedDateTime,
    val values: List<PublicDisciplineEvaluationData>
)
