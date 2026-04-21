package dev.forcetower.melon.feature.overview.domain.usecase

import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.query.StudentDisciplineRow
import dev.forcetower.melon.feature.overview.domain.internal.pickActiveSemester
import dev.forcetower.melon.feature.overview.domain.model.OverviewDiscipline
import dev.forcetower.melon.feature.overview.domain.model.OverviewDisciplineStatus
import dev.zacsweers.metro.Inject
import kotlin.math.round
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class ObserveDisciplinesUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
) {
    private val timeZone = TimeZone.currentSystemDefault()

    operator fun invoke(): Flow<List<OverviewDiscipline>> =
        semesterDao.observeAll().flatMapLatest { semesters ->
            val today = Clock.System.now().toLocalDateTime(timeZone).date.toString()
            val semester = pickActiveSemester(semesters, today)
                ?: return@flatMapLatest flowOf(emptyList())
            academicDao.observeStudentDisciplines(semester.id).map { rows ->
                rows.map { it.toDomain(semester.code) }
            }
        }
}

private fun StudentDisciplineRow.toDomain(semesterCode: String): OverviewDiscipline {
    val finalValue = finalGrade?.replace(",", ".")?.toDoubleOrNull()
    val hasFinal = finalValue != null
    val status = when {
        hasFinal && approved == true -> OverviewDisciplineStatus.APROVADO
        hasFinal && approved == false -> OverviewDisciplineStatus.REPROVADO
        hasFinal -> OverviewDisciplineStatus.FINAL
        else -> OverviewDisciplineStatus.PARCIAL
    }
    val displayValue = finalValue ?: weightedAverage
    val label = displayValue?.let { formatGrade(it) } ?: "—"
    return OverviewDiscipline(
        disciplineId = disciplineId,
        offerId = offerId,
        code = code,
        title = name,
        gradeLabel = label,
        status = status,
        semesterCode = semesterCode,
    )
}

// "8,5" — round to 1 decimal, comma separator (PT-BR). "10,0" and "0,0" stay
// two-char whole parts to match the design's visual width.
private fun formatGrade(value: Double): String {
    val clamped = value.coerceIn(0.0, 10.0)
    val scaled = round(clamped * 10).toInt()
    val whole = scaled / 10
    val tenth = scaled % 10
    return "$whole,$tenth"
}
