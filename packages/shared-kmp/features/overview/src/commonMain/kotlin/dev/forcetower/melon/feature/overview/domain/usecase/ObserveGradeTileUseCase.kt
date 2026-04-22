package dev.forcetower.melon.feature.overview.domain.usecase

import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.query.EnrolledDisciplineRow
import dev.forcetower.melon.core.database.query.PartialGradeRow
import dev.forcetower.melon.feature.overview.domain.internal.pickActiveSemester
import dev.forcetower.melon.feature.overview.domain.model.OverviewGradeTile
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Mirrors the CR calculation ObserveMeProfileUseCase performs for the "Eu"
// hero, scoped to the overview's active semester. Reading the same DAO
// streams (enrolled disciplines + every partial grade) guarantees the number
// stays in lockstep with the Me tab — a student sees one CR across the app.
// The two use cases intentionally duplicate the tiny weighted-average helper
// instead of reaching across feature modules; see the same note in
// ObserveDisciplinesListUseCase.
@Inject
class ObserveGradeTileUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
) {
    private val timeZone = TimeZone.currentSystemDefault()

    operator fun invoke(): Flow<OverviewGradeTile?> = combine(
        semesterDao.observeAll(),
        academicDao.observeAllEnrolledDisciplines(),
        academicDao.observeAllPartialGrades(),
    ) { semesters, enrollments, grades ->
        val today = Clock.System.now().toLocalDateTime(timeZone).date.toString()
        buildTile(semesters, enrollments, grades, today)
    }.distinctUntilChanged()
}

private fun buildTile(
    semesters: List<SemesterEntity>,
    enrollments: List<EnrolledDisciplineRow>,
    grades: List<PartialGradeRow>,
    todayIso: String,
): OverviewGradeTile? {
    val active = pickActiveSemester(semesters, todayIso) ?: return null
    val enrollmentsBySemester = enrollments.groupBy { it.semesterId }
    val gradesByStudentClass = grades.groupBy { it.studentClassId }

    val activeCR = semesterWeightedAverage(
        enrollmentsBySemester[active.id],
        gradesByStudentClass,
    ) ?: return null

    val previous = semesters
        .asSequence()
        .filter { it.id != active.id && it.id in enrollmentsBySemester }
        .sortedByDescending { it.endDate }
        .firstOrNull()
    val previousCR = previous?.id?.let {
        semesterWeightedAverage(enrollmentsBySemester[it], gradesByStudentClass)
    }
    return OverviewGradeTile(
        cr = activeCR,
        crDelta = if (previousCR != null) activeCR - previousCR else null,
        comparisonSemesterCode = previous?.code?.takeIf { previousCR != null },
    )
}

private fun semesterWeightedAverage(
    enrollments: List<EnrolledDisciplineRow>?,
    gradesByStudentClass: Map<String, List<PartialGradeRow>>,
): Double? {
    val rows = enrollments ?: return null
    // Dedup by upstream id so multi-group disciplines (same grade set replicated
    // per StudentClass) aren't double-weighted when rolled up into CR.
    val all = rows
        .flatMap { gradesByStudentClass[it.studentClassId].orEmpty() }
        .distinctBy { it.gradePlatformId }
    return weightedAverage(all)
}

// Duplicated from ObserveMeProfileUseCase — same 3-line helper; keep in
// lockstep if the weighting ever changes.
private fun weightedAverage(grades: List<PartialGradeRow>): Double? {
    var weightedSum = 0.0
    var weightSum = 0.0
    for (grade in grades) {
        val value = grade.parsedValue() ?: continue
        val weight = grade.weight.replace(",", ".").toDoubleOrNull() ?: continue
        weightedSum += value * weight
        weightSum += weight
    }
    return if (weightSum > 0.0) weightedSum / weightSum else null
}

private fun PartialGradeRow.parsedValue(): Double? =
    value?.takeIf { it.isNotBlank() }?.replace(",", ".")?.toDoubleOrNull()
