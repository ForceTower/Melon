package dev.forcetower.melon.feature.overview.domain.usecase

import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.query.EnrolledDisciplineRow
import dev.forcetower.melon.feature.overview.domain.internal.pickActiveSemester
import dev.forcetower.melon.feature.overview.domain.model.OverviewGradeTile
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Lifetime CR for the grade tile, plus a delta showing how much the active
// semester is moving it. The delta is `overall − overall capped at the active
// semester start` — i.e. the contribution of in-progress final grades.
// `comparisonSemesterCode` is the most recent semester strictly before the
// active one. The math mirrors CalculateOverallScoreUseCase; the helper is
// duplicated here on purpose (cross-feature `internal` doesn't carry, and
// the codebase already prefers small local copies over a shared module).
@Inject
class ObserveGradeTileUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
) {
    private val timeZone = TimeZone.currentSystemDefault()

    operator fun invoke(): Flow<OverviewGradeTile?> = combine(
        semesterDao.observeAll(),
        academicDao.observeAllEnrolledDisciplines(),
    ) { semesters, enrollments ->
        val today = Clock.System.now().toLocalDateTime(timeZone).date.toString()
        buildTile(semesters, enrollments, today)
    }.distinctUntilChanged()
}

private fun buildTile(
    semesters: List<SemesterEntity>,
    enrollments: List<EnrolledDisciplineRow>,
    todayIso: String,
): OverviewGradeTile? {
    val overall = overallScore(semesters, enrollments, capSemesterId = null) ?: return null
    val active = pickActiveSemester(semesters, todayIso)
    val before = active?.let { overallScore(semesters, enrollments, capSemesterId = it.id) }
    val previousCode = active?.let { a ->
        semesters
            .asSequence()
            .filter { it.id != a.id && it.startDate < a.startDate }
            .maxByOrNull { it.startDate }
            ?.code
    }
    return OverviewGradeTile(
        cr = overall,
        crDelta = before?.let { overall - it },
        comparisonSemesterCode = previousCode?.takeIf { before != null },
    )
}

private fun overallScore(
    semesters: List<SemesterEntity>,
    enrollments: List<EnrolledDisciplineRow>,
    capSemesterId: String?,
): Double? {
    val allowed: Set<String> = if (capSemesterId == null) {
        semesters.mapTo(mutableSetOf()) { it.id }
    } else {
        val cap = semesters.firstOrNull { it.id == capSemesterId } ?: return null
        semesters.asSequence()
            .filter { it.startDate < cap.startDate }
            .mapTo(mutableSetOf()) { it.id }
    }

    val contributions = enrollments
        .asSequence()
        .filter { it.semesterId in allowed }
        .mapNotNull { row ->
            val grade = row.finalGrade?.replace(",", ".")?.toDoubleOrNull() ?: return@mapNotNull null
            row to grade
        }
        .distinctBy { (row, _) -> row.offerId }
        .toList()

    var weightedSum = 0.0
    var weightSum = 0.0
    for ((row, grade) in contributions) {
        val hours = row.disciplineHours
        if (hours <= 0) continue
        weightedSum += grade * hours
        weightSum += hours
    }
    return if (weightSum > 0.0) weightedSum / weightSum else null
}
