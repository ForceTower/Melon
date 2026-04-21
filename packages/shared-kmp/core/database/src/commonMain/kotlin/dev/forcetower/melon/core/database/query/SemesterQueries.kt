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

// Semester-wide class-hours tally paired with the hours already elapsed. Each
// class contributes `c.hours × (past lectures / total lectures)` to
// `completedHours`; classes without scheduled lectures contribute zero. The Me
// tab renders this as "X de Y horas" for the current semester.
data class SemesterHoursProgressRow(
    val totalHours: Int,
    val completedHours: Double,
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

// One row per StudentClass (the student's enrollment in a class/group). The
// disciplines list view aggregates these in Kotlin by (semesterId, offerId)
// because a single discipline may have multiple groups (theory + practice)
// the student is enrolled in at once. `disciplineHours` is the offer's hours
// when present and falls back to the catalog discipline's hours.
data class EnrolledDisciplineRow(
    val studentClassId: String,
    val classId: String,
    val classType: String,
    val groupName: String,
    val classHours: Int,
    val offerId: String,
    val semesterId: String,
    val disciplineHours: Int,
    val disciplineId: String,
    val disciplineCode: String,
    val disciplineName: String,
    val department: String?,
    val finalGrade: String?,
    val approved: Boolean?,
    val missedClasses: Int?,
    val teacherName: String?,
)

// Every StudentGrade row — the local DB only stores the signed-in student's
// grades so no filter is needed. `value` and `weight` are strings upstream,
// parsed client-side. `date` is ISO yyyy-MM-dd.
data class PartialGradeRow(
    val gradeId: String,
    val studentClassId: String,
    val name: String,
    val nameShort: String?,
    val ordinal: Int,
    val weight: String,
    val value: String?,
    val date: String?,
)

// One row per StudentClass enrolled under a single DisciplineOffer — the
// detail-view use case aggregates these by classId so multi-group disciplines
// (theory + practice) produce one entry per class. `disciplineProgram` carries
// the ementa (upstream `ementa` is mapped into `program` by the TS mappers).
data class DisciplineDetailEnrollmentRow(
    val offerId: String,
    val semesterId: String,
    val disciplineId: String,
    val disciplineCode: String,
    val disciplineName: String,
    val disciplineHours: Int,
    val disciplineProgram: String?,
    val department: String?,
    val offerHours: Int?,
    val studentClassId: String,
    val classId: String,
    val classType: String,
    val groupName: String,
    val classHours: Int,
    val finalGrade: String?,
    val approved: Boolean?,
    val missedClasses: Int?,
    val teacherName: String?,
)

// StudentGrade rows joined with their ClassEvaluation metadata, scoped to a
// single DisciplineOffer. Ordered (classId, evaluation.position, ordinal) so
// the use case can slice by classId and preserve evaluation order for the UI.
// `gradePlatformId` is the upstream grade id — stable across the per-class
// replication that `applyDiscipline` performs on the backend, so it's the
// dedup key the use case uses to collapse multi-group disciplines' grades
// back to the upstream's single shared set.
data class DisciplineDetailGradeRow(
    val gradeId: String,
    val gradePlatformId: String,
    val studentClassId: String,
    val classId: String,
    val evaluationId: String,
    val evaluationName: String?,
    val evaluationPosition: Int,
    val gradeName: String,
    val gradeNameShort: String?,
    val ordinal: Int,
    val weight: String,
    val value: String?,
    val date: String?,
)

// One row per ClassLecture in the offer with the attachment count pulled in via
// a correlated subquery — keeps the projection one-row-per-lecture instead of
// fanning out across materials.
data class DisciplineDetailLectureRow(
    val lectureId: String,
    val classId: String,
    val ordinal: Int,
    val situation: Int,
    val date: String?,
    val subject: String?,
    val attachmentCount: Int,
)

// LectureMaterial joined with its lecture's date + enclosing classId. The
// detail view renders these as the attachments block and groups them by
// class when the discipline runs multiple groups. `caption` is aliased from
// the `description` column to keep the SKIE-generated Swift API clear of
// `NSObject.description` ambiguity.
data class DisciplineDetailMaterialRow(
    val materialId: String,
    val lectureId: String,
    val classId: String,
    val lectureDate: String?,
    val caption: String?,
    val url: String,
)
