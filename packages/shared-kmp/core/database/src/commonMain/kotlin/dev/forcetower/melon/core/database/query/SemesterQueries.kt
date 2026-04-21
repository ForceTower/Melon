package dev.forcetower.melon.core.database.query

// Projection returned by AcademicDao.getSemesterAggregate. `totalHours` sums
// Class.hours across every class tied to the semester — displayed in the UI as
// "créditos".
data class SemesterClassAggregate(
    val classCount: Int,
    val totalHours: Int,
)

// One row per ClassAllocation, pre-joined with the enclosing class's
// discipline, primary teacher, and space. `day` follows the upstream encoding
// (stored as-is from the source platform, 1=Sunday..7=Saturday).
// `disciplineCode` is the short code shown as a badge (e.g. "CALC II") —
// already present on Discipline. Space fields are LEFT-joined and null when
// the allocation has no space attached (e.g. online classes before the
// campus/modulo resolve).
data class SemesterAllocationRow(
    val allocationId: String,
    val classId: String,
    val disciplineCode: String,
    val disciplineName: String,
    val day: Int?,
    val startTime: String?,
    val endTime: String?,
    val spaceLocation: String?,
    val spaceCampus: String?,
    val spaceModulo: String?,
    val teacherName: String?,
)

// Per-lecture projection for a date range — used by the Schedule week view to
// enrich each day's allocations with the lecture topic for that exact date.
// Joined on (classId, date) client-side; no assumption of uniqueness since
// the same class can have multiple lectures on a given day.
data class WeekLectureRow(
    val classId: String,
    val date: String,
    val subject: String?,
)

// Per-enrollment discipline projection for the Overview disciplines strip and
// for grade-status classification. `weightedAverage` sums only graded rows,
// so it's the running partial grade — null when no grade has landed yet.
data class StudentDisciplineRow(
    val disciplineId: String,
    val code: String,
    val name: String,
    val finalGrade: String?,
    val approved: Boolean?,
    val weightedAverage: Double?,
)

// Per-lecture projection filtered to a specific class+date; used to resolve
// the `topic` text shown on NowCard and the timeline.
data class TodayLectureRow(
    val classId: String,
    val subject: String?,
)

// Closest upcoming evaluation — derived from student_grade rows whose date is
// still in the future and whose value hasn't been posted. Heuristic until
// upstream exposes real evaluation dates.
data class UpcomingEvaluationRow(
    val date: String,
    val evaluationName: String?,
    val disciplineCode: String,
    val disciplineName: String,
)

// Aggregate miss/hours across the semester. Percentage and allowed-absences
// (75% rule) are computed from this on the client.
data class AttendanceSummaryRow(
    val totalMissed: Int,
    val totalHours: Int,
)

// Raw recent lectures (situation = upstream attendance code). Client bucks
// these into a "past N class-days" strip. situation==0 means present.
data class RecentLectureRow(
    val date: String,
    val situation: Int,
)

// Latest unread message (LEFT JOIN MessageState — a missing state row is
// treated as unread, matching MirrorRepositoryImpl's "fresh inbox lands as
// unread" contract).
data class UnreadMessageHeadRow(
    val senderName: String,
    val subject: String?,
    val content: String,
)
