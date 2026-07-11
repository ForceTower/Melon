package dev.forcetower.melon.feature.disciplines.domain.usecase

import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.query.EnrolledDisciplineRow
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

// Lifetime CR — weighted mean of every completed discipline's final grade
// using its hour count as the weight. A discipline counts when its final
// grade is set, regardless of approval. Multi-group enrollments collapse to
// one contribution via `offerId`. The optional `capSemesterId` clips the
// pool to semesters that started strictly before the cap, letting callers
// ask "what was the CR up to semester X?".
@Inject
class CalculateOverallScoreUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
) {
    operator fun invoke(capSemesterId: String? = null): Flow<Double?> = combine(
        semesterDao.observeAll(),
        academicDao.observeAllEnrolledDisciplines(),
    ) { semesters, enrollments ->
        computeOverallScore(semesters, enrollments, capSemesterId)
    }.distinctUntilChanged()

    // Lifetime CR paired with its latest movement: `delta` = CR now − CR as
    // it stood before the most recent semester with closed grades. Same
    // semantics as the iOS sparkline delta (CoefficientHistory.swift); delta
    // stays null until two semesters have closed grades.
    fun summary(): Flow<OverallScoreSummary> = combine(
        semesterDao.observeAll(),
        academicDao.observeAllEnrolledDisciplines(),
    ) { semesters, enrollments ->
        computeOverallScoreSummary(semesters, enrollments)
    }.distinctUntilChanged()
}

data class OverallScoreSummary(
    val value: Double?,
    val delta: Double?,
)

internal fun computeOverallScoreSummary(
    semesters: List<SemesterEntity>,
    enrollments: List<EnrolledDisciplineRow>,
): OverallScoreSummary {
    val value = computeOverallScore(semesters, enrollments, capSemesterId = null)
    val lastClosed = semesters
        .filter { semester ->
            enrollments.any {
                it.semesterId == semester.id &&
                    it.disciplineHours > 0 &&
                    it.finalGrade?.replace(",", ".")?.toDoubleOrNull() != null
            }
        }
        .maxByOrNull { it.startDate }
    val before = lastClosed?.let { computeOverallScore(semesters, enrollments, capSemesterId = it.id) }
    return OverallScoreSummary(
        value = value,
        delta = if (value != null && before != null) value - before else null,
    )
}

internal fun computeOverallScore(
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
