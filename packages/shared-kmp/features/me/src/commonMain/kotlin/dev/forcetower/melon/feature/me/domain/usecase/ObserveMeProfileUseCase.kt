package dev.forcetower.melon.feature.me.domain.usecase

import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.CredentialsDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.dao.StudentDao
import dev.forcetower.melon.core.database.dao.UserDao
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.entity.StudentEntity
import dev.forcetower.melon.core.database.entity.UserEntity
import dev.forcetower.melon.core.database.query.EnrolledDisciplineRow
import dev.forcetower.melon.core.database.query.PartialGradeRow
import dev.forcetower.melon.core.database.query.SemesterHoursProgressRow
import dev.forcetower.melon.core.database.query.UpcomingEvaluationRow
import dev.forcetower.melon.feature.me.domain.model.MeEnrollmentSummary
import dev.forcetower.melon.feature.me.domain.model.MeIdentity
import dev.forcetower.melon.feature.me.domain.model.MeNextExam
import dev.forcetower.melon.feature.me.domain.model.MeProfile
import dev.forcetower.melon.feature.me.domain.model.MeSemesterProgress
import dev.zacsweers.metro.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

// One flow for the entire "Eu" hero + semester strip. Base slice (user,
// student, semesters, enrollments, grades) is broad because CR/crDelta need
// per-semester grade rollups across history. When an active semester exists
// we flatMap into the two additional semester-scoped flows
// (hours progress + next evaluation); otherwise we still emit a profile so
// the header can render without semester context.
@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class ObserveMeProfileUseCase internal constructor(
    private val userDao: UserDao,
    private val studentDao: StudentDao,
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
    private val credentialsDao: CredentialsDao,
) {
    private val timeZone = TimeZone.currentSystemDefault()

    operator fun invoke(): Flow<MeProfile> {
        val baseProfile = combine(
            userDao.observeCurrent(),
            studentDao.observeCurrent(),
            semesterDao.observeAll(),
            academicDao.observeAllEnrolledDisciplines(),
            academicDao.observeAllPartialGrades(),
        ) { user, student, semesters, enrollments, grades ->
            ProfileSlice(user, student, semesters, enrollments, grades)
        }.flatMapLatest { slice ->
            val today = Clock.System.now().toLocalDateTime(timeZone).date.toString()
            val active = pickActiveSemester(slice.semesters, today)
            if (active == null) {
                flow {
                    emit(buildProfile(slice, active = null, hours = null, nextExam = null, today = today))
                }
            } else {
                combine(
                    academicDao.observeSemesterHoursProgress(active.id, today),
                    academicDao.observeClosestUpcomingEvaluation(active.id, today),
                ) { hours, next ->
                    buildProfile(slice, active = active, hours = hours, nextExam = next, today = today)
                }
            }
        }
        // Folded in as an outer combine rather than a sixth source on the
        // base-slice combine: the `combine` overload tops out at five typed
        // args, and the credentials row is independent of the active-semester
        // flatMap branch.
        return combine(baseProfile, credentialsDao.observeCurrent()) { profile, credentials ->
            profile.copy(identity = profile.identity.copy(username = credentials?.username))
        }.distinctUntilChanged()
    }

    private suspend fun buildProfile(
        slice: ProfileSlice,
        active: SemesterEntity?,
        hours: SemesterHoursProgressRow?,
        nextExam: UpcomingEvaluationRow?,
        today: String,
    ): MeProfile {
        val courseName = slice.student?.courseId?.let { studentDao.getCourse(it)?.name }
        return MeProfile(
            identity = buildIdentity(slice.user, slice.student, courseName),
            semester = active?.let { buildSemesterProgress(it, today) },
            enrollment = buildEnrollmentSummary(slice, active, hours),
            nextExam = nextExam?.let {
                MeNextExam(
                    date = it.date,
                    evaluationName = it.evaluationName,
                    disciplineCode = it.disciplineCode,
                    disciplineName = it.disciplineName,
                )
            },
        )
    }

    private fun buildIdentity(
        user: UserEntity?,
        student: StudentEntity?,
        courseName: String?,
    ): MeIdentity {
        val canonicalName = user?.name?.takeIf { it.isNotBlank() }
            ?: student?.name.orEmpty()
        val first = canonicalName.substringBefore(' ').ifBlank { canonicalName }
        return MeIdentity(
            userName = canonicalName,
            firstName = first,
            courseName = courseName,
            enrollmentNumber = student?.platformId?.toString().orEmpty(),
            // Filled in by the outer combine over `credentialsDao.observeCurrent()`.
            username = null,
            avatarUrl = user?.imageUrl,
        )
    }

    private fun buildSemesterProgress(semester: SemesterEntity, todayIso: String): MeSemesterProgress {
        val start = runCatching { LocalDate.parse(semester.startDate) }.getOrNull()
        val end = runCatching { LocalDate.parse(semester.endDate) }.getOrNull()
        val today = runCatching { LocalDate.parse(todayIso) }.getOrNull()
        val totalDays = if (start != null && end != null) start.daysUntil(end).coerceAtLeast(1) else 1
        val elapsedDays = if (start != null && today != null) {
            start.daysUntil(today).coerceIn(0, totalDays)
        } else {
            0
        }
        // Ceiling division so a 125-day semester reports 18 weeks, matching the
        // UEFS calendar the fixture is modelled on.
        val totalWeeks = ((totalDays + 6) / 7).coerceAtLeast(1)
        val currentWeek = (elapsedDays / 7 + 1).coerceIn(1, totalWeeks)
        val percent = ((elapsedDays.toDouble() / totalDays.toDouble()) * 100).roundToInt()
            .coerceIn(0, 100)
        return MeSemesterProgress(
            semesterId = semester.id,
            code = semester.code,
            startDate = semester.startDate,
            endDate = semester.endDate,
            currentWeek = currentWeek,
            totalWeeks = totalWeeks,
            progressPercent = percent,
        )
    }

    private fun buildEnrollmentSummary(
        slice: ProfileSlice,
        active: SemesterEntity?,
        hours: SemesterHoursProgressRow?,
    ): MeEnrollmentSummary {
        val enrollmentsBySemester = slice.enrollments.groupBy { it.semesterId }
        val gradesByStudentClass = slice.grades.groupBy { it.studentClassId }

        val activeCR = active?.id?.let { sid ->
            semesterWeightedAverage(enrollmentsBySemester[sid], gradesByStudentClass)
        }
        val previousSemester = slice.semesters
            .asSequence()
            .filter { it.id != active?.id && it.id in enrollmentsBySemester }
            .sortedByDescending { it.endDate }
            .firstOrNull()
        val previousCR = previousSemester?.id?.let { sid ->
            semesterWeightedAverage(enrollmentsBySemester[sid], gradesByStudentClass)
        }
        val crDelta = if (activeCR != null && previousCR != null) activeCR - previousCR else null

        return MeEnrollmentSummary(
            cr = activeCR,
            crDelta = crDelta,
            completedHours = hours?.completedHours?.roundToInt() ?: 0,
            totalHours = hours?.totalHours ?: 0,
        )
    }

    private fun semesterWeightedAverage(
        enrollments: List<EnrolledDisciplineRow>?,
        gradesByStudentClass: Map<String, List<PartialGradeRow>>,
    ): Double? {
        val rows = enrollments ?: return null
        // Dedup by upstream id so multi-group disciplines (same grade set
        // replicated per StudentClass) aren't double-weighted in CR.
        val grades = rows
            .flatMap { gradesByStudentClass[it.studentClassId].orEmpty() }
            .distinctBy { it.gradePlatformId }
        return weightedAverage(grades)
    }

    private data class ProfileSlice(
        val user: UserEntity?,
        val student: StudentEntity?,
        val semesters: List<SemesterEntity>,
        val enrollments: List<EnrolledDisciplineRow>,
        val grades: List<PartialGradeRow>,
    )
}

// Same active-semester rule as ObserveDisciplinesUseCase — today inside
// [startDate, endDate]; otherwise fall back to the most recently started.
private fun pickActiveSemester(all: List<SemesterEntity>, todayIso: String): SemesterEntity? {
    if (all.isEmpty()) return null
    return all.firstOrNull { it.startDate <= todayIso && todayIso <= it.endDate }
        ?: all.maxByOrNull { it.startDate }
}

// Duplicated from ObserveDisciplinesListUseCase — a 2-function helper isn't
// worth a dependency between feature modules. Keep in lockstep.
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
