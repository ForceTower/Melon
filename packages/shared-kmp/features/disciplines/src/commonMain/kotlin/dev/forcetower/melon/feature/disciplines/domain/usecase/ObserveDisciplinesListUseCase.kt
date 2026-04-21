package dev.forcetower.melon.feature.disciplines.domain.usecase

import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.query.EnrolledDisciplineRow
import dev.forcetower.melon.core.database.query.PartialGradeRow
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineListItem
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineStatusKind
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplinesListState
import dev.forcetower.melon.feature.disciplines.domain.model.PendingSemester
import dev.forcetower.melon.feature.disciplines.domain.model.SemesterDisciplines
import dev.zacsweers.metro.Inject
import kotlin.math.ceil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Reactive feed for the Disciplinas screen. Combines the full semester list
// (metadata) with every enrolled StudentClass row and every StudentGrade row
// so one emission carries the complete screen state. Grouping happens in
// Kotlin: the SQL stays simple and composable with the Overview/Schedule
// queries that already read the same tables.
@Inject
class ObserveDisciplinesListUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
) {
    private val timeZone = TimeZone.currentSystemDefault()

    operator fun invoke(): Flow<DisciplinesListState> = combine(
        semesterDao.observeAll(),
        academicDao.observeAllEnrolledDisciplines(),
        academicDao.observeAllPartialGrades(),
    ) { semesters, enrollments, grades ->
        val today = Clock.System.now().toLocalDateTime(timeZone).date.toString()
        buildState(semesters, enrollments, grades, today)
    }.distinctUntilChanged()
}

private fun buildState(
    semesters: List<SemesterEntity>,
    enrollments: List<EnrolledDisciplineRow>,
    grades: List<PartialGradeRow>,
    today: String,
): DisciplinesListState {
    if (semesters.isEmpty()) {
        return DisciplinesListState(current = null, past = emptyList(), pending = emptyList())
    }

    val gradesByStudentClass = grades.groupBy { it.studentClassId }
    val itemsByOffer = enrollments
        .groupBy { it.offerId }
        .mapValues { (_, rows) -> buildItem(rows, gradesByStudentClass) }
    val itemsBySemester = itemsByOffer.values.groupBy { it.semesterId }
    val downloadedSemesterIds = itemsBySemester.keys

    val current = semesters
        .firstOrNull { it.id in downloadedSemesterIds && today in it.startDate..it.endDate }
        ?.let { it.toGroup(itemsBySemester.getValue(it.id)) }

    val past = semesters
        .asSequence()
        .filter { it.id in downloadedSemesterIds && it.id != current?.semesterId }
        .sortedByDescending { it.endDate }
        .map { it.toGroup(itemsBySemester.getValue(it.id)) }
        .toList()

    val pending = semesters
        .asSequence()
        .filter { it.id !in downloadedSemesterIds }
        .sortedByDescending { it.endDate }
        .map { PendingSemester(semesterId = it.id, semesterCode = it.code) }
        .toList()

    return DisciplinesListState(current = current, past = past, pending = pending)
}

private fun SemesterEntity.toGroup(items: List<DisciplineListItem>): SemesterDisciplines =
    SemesterDisciplines(
        semesterId = id,
        semesterCode = code,
        disciplines = items.sortedBy { it.code },
    )

// Merges every StudentClass row belonging to the same DisciplineOffer into
// one card-ready item. All rows in `rows` share (semesterId, disciplineId,
// offerId) by construction.
private fun buildItem(
    rows: List<EnrolledDisciplineRow>,
    gradesByStudentClass: Map<String, List<PartialGradeRow>>,
): DisciplineListItem {
    val head = rows.first()
    val studentClassIds = rows.map { it.studentClassId }
    val hours = rows.sumOf { it.classHours }.takeIf { it > 0 } ?: head.disciplineHours
    val missedHours = rows.sumOf { it.missedClasses ?: 0 }
    val allowedMissedHours = ceil(hours * 0.25).toInt()

    val allGrades = studentClassIds.flatMap { gradesByStudentClass[it].orEmpty() }
    val partialAverage = weightedAverage(allGrades)
    val completedEvaluations = allGrades.count { it.parsedValue() != null }
    val nextEvaluation = allGrades
        .filter { it.parsedValue() == null && !it.date.isNullOrBlank() }
        .minByOrNull { it.date!! }

    // finalGrade / approved typically land on one of the StudentClass rows
    // (the discipline's main group). Take the first non-null.
    val finalGradeString = rows.firstOrNull { !it.finalGrade.isNullOrBlank() }?.finalGrade
    val finalGrade = finalGradeString?.replace(",", ".")?.toDoubleOrNull()
    val approved = rows.firstOrNull { it.approved != null }?.approved

    val groupsLabel = buildGroupsLabel(rows)

    return DisciplineListItem(
        disciplineId = head.disciplineId,
        offerId = head.offerId,
        semesterId = head.semesterId,
        studentClassIds = studentClassIds,
        code = head.disciplineCode,
        name = head.disciplineName,
        department = head.department,
        teacherName = rows.firstNotNullOfOrNull { it.teacherName },
        hours = hours,
        missedHours = missedHours,
        allowedMissedHours = allowedMissedHours,
        partialAverage = partialAverage,
        finalGrade = finalGrade,
        approved = approved,
        status = classifyStatus(
            approved = approved,
            finalGrade = finalGrade,
            partialAverage = partialAverage,
        ),
        groupsLabel = groupsLabel,
        totalEvaluations = allGrades.size,
        completedEvaluations = completedEvaluations,
        nextEvaluationTitle = nextEvaluation?.let { it.nameShort ?: it.name },
        nextEvaluationDateIso = nextEvaluation?.date,
    )
}

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

// Two-letter slugs joined with " · " — matches the iOS card label format.
// Deduplicates so the same type appearing on multiple groups shows once.
private fun buildGroupsLabel(rows: List<EnrolledDisciplineRow>): String? {
    if (rows.size <= 1) return null
    val slugs = rows
        .asSequence()
        .map { it.classType.trim() }
        .filter { it.isNotEmpty() }
        .map { it.take(2) }
        .distinct()
        .toList()
    if (slugs.size <= 1) return null
    return slugs.joinToString(separator = " · ")
}

private const val PASS_THRESHOLD = 5.5

private fun classifyStatus(
    approved: Boolean?,
    finalGrade: Double?,
    partialAverage: Double?,
): DisciplineStatusKind = when {
    approved == true -> DisciplineStatusKind.APROVADO
    approved == false -> DisciplineStatusKind.REPROVADO
    finalGrade != null -> DisciplineStatusKind.FINAL
    partialAverage == null -> DisciplineStatusKind.PENDING
    partialAverage < PASS_THRESHOLD -> DisciplineStatusKind.LOW
    else -> DisciplineStatusKind.ONGOING
}
