package dev.forcetower.melon.feature.disciplines.domain.usecase

import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.query.EnrolledDisciplineRow
import dev.forcetower.melon.feature.disciplines.domain.model.OverallScore
import dev.forcetower.melon.feature.disciplines.domain.model.ProgramScore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

// Lifetime CR — weighted mean of every completed discipline's final grade
// using its hour count as the weight. A discipline counts when its final
// grade is set, regardless of approval. Multi-group enrollments collapse to
// one contribution via `offerId`.
//
// The mean is computed per program, never across them: a student who finished
// a graduação and moved on to a mestrado has two CRs, and blending them
// produces a number that belongs to neither. Programs are keyed by the
// semester track — see `ProgramScore`.
@Inject
class CalculateOverallScoreUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
) {
    operator fun invoke(): Flow<OverallScore> = combine(
        semesterDao.observeAll(),
        academicDao.observeAllEnrolledDisciplines(),
    ) { semesters, enrollments ->
        computeOverallScore(semesters, enrollments)
    }.distinctUntilChanged()

    // One program's CR after each of its semesters that closed anything,
    // oldest first — the sparkline series. Callers decide what to do with a
    // series too short to draw.
    fun checkpoints(track: String?): Flow<List<Double>> = combine(
        semesterDao.observeAll(),
        academicDao.observeAllEnrolledDisciplines(),
    ) { semesters, enrollments ->
        computeCheckpoints(semesters, enrollments, track)
    }.distinctUntilChanged()
}

internal fun computeOverallScore(
    semesters: List<SemesterEntity>,
    enrollments: List<EnrolledDisciplineRow>,
): OverallScore {
    val semesterById = semesters.associateBy { it.id }
    val programs = closedContributions(semesterById, enrollments)
        .groupBy { it.semester.track }
        .entries
        .sortedByDescending { (_, rows) -> rows.maxOf { it.semester.startDate } }
        .mapNotNull { (track, rows) -> programScore(track, rows) }

    return OverallScore(programs = programs, currentTrack = currentTrack(semesterById, enrollments))
}

internal fun computeCheckpoints(
    semesters: List<SemesterEntity>,
    enrollments: List<EnrolledDisciplineRow>,
    track: String?,
): List<Double> {
    val semesterById = semesters.associateBy { it.id }
    val bySemester = closedContributions(semesterById, enrollments)
        .filter { it.semester.track == track }
        .groupBy { it.semester }

    var weightedSum = 0.0
    var weightSum = 0.0
    val points = mutableListOf<Double>()
    for (semester in bySemester.keys.sortedBy { it.startDate }) {
        for (row in bySemester.getValue(semester)) {
            weightedSum += row.grade * row.hours
            weightSum += row.hours
        }
        if (weightSum > 0.0) points += weightedSum / weightSum
    }
    return points
}

// Grade parsing runs before the per-offer dedup on purpose: a discipline that
// runs as theory + practice carries its final grade on one of the two rows,
// so deduping first would coin-flip the graded row away.
private fun closedContributions(
    semesterById: Map<String, SemesterEntity>,
    enrollments: List<EnrolledDisciplineRow>,
): List<Contribution> = enrollments
    .asSequence()
    .mapNotNull { row ->
        val grade = row.finalGrade?.replace(",", ".")?.toDoubleOrNull() ?: return@mapNotNull null
        row to grade
    }
    .distinctBy { (row, _) -> row.offerId }
    .mapNotNull { (row, grade) ->
        val semester = semesterById[row.semesterId] ?: return@mapNotNull null
        if (row.disciplineHours <= 0) return@mapNotNull null
        Contribution(semester = semester, grade = grade, hours = row.disciplineHours)
    }
    .toList()

// The program the student is in right now — the track of the newest semester
// they hold enrollments in, closed grades or not. Read off enrollments rather
// than the semester list, which also carries semesters never downloaded.
private fun currentTrack(
    semesterById: Map<String, SemesterEntity>,
    enrollments: List<EnrolledDisciplineRow>,
): String? = enrollments
    .asSequence()
    .mapNotNull { semesterById[it.semesterId] }
    .maxByOrNull { it.startDate }
    ?.track

private fun programScore(track: String?, rows: List<Contribution>): ProgramScore? {
    val value = weightedMean(rows) ?: return null
    val lastClosedStart = rows.maxOf { it.semester.startDate }
    val before = weightedMean(rows.filter { it.semester.startDate < lastClosedStart })
    return ProgramScore(
        track = track,
        value = value,
        delta = before?.let { value - it },
        semesterCount = rows.distinctBy { it.semester.id }.size,
        disciplineCount = rows.size,
    )
}

private fun weightedMean(rows: List<Contribution>): Double? {
    var weightedSum = 0.0
    var weightSum = 0.0
    for (row in rows) {
        weightedSum += row.grade * row.hours
        weightSum += row.hours
    }
    return if (weightSum > 0.0) weightedSum / weightSum else null
}

private data class Contribution(
    val semester: SemesterEntity,
    val grade: Double,
    val hours: Int,
)
