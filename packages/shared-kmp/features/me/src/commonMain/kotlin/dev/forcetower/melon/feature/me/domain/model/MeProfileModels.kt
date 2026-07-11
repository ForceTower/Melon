package dev.forcetower.melon.feature.me.domain.model

// Aggregate payload for the "Eu" tab. One emission carries everything the
// screen needs except the shortcut grid / settings rows, which stay iOS-owned.
// `semester` and `enrollment` are nullable so the UI can render the hero even
// before a semester is synced or the active one is past its end date.
data class MeProfile(
    val identity: MeIdentity,
    val semester: MeSemesterProgress?,
    val enrollment: MeEnrollmentSummary,
    val nextExam: MeNextExam?,
    // "Campus · Módulo" where the student's classes most often meet — the
    // modal (most frequent) campus/módulo across the active semester's
    // allocations, mirroring iOS `campusLabel`. Null when no space is known.
    val campus: String? = null,
    // Attendance across the active semester: 100 − missed/hours. Null until
    // at least one lecture has happened (a fresh semester would otherwise
    // open at a meaningless 100%).
    val attendancePercent: Int? = null,
    // 1-based position of the active semester among the semesters the
    // student has enrollments in ("6º semestre"). Null without an active
    // semester.
    val semesterOrdinal: Int? = null,
)

// Student-facing profile fields. `campus` is intentionally absent: every
// upstream today is UEFS, so the iOS mapping hardcodes it.
//
// `username` is the typed login string from the credentials row; nullable for
// the brief window between the user upserting and the credentials flow
// catching up (and for the legacy v4 → v5 destructive migration where the
// user is sent back to onboarding anyway).
data class MeIdentity(
    val userName: String,
    val firstName: String,
    val courseName: String?,
    val enrollmentNumber: String,
    val username: String?,
    val avatarUrl: String?,
)

// Timeline metadata for the active semester. Week/total/percent are computed
// from today vs. [startDate, endDate]; when today falls outside that window we
// still emit the boundary values so the UI can render "0/18" / "18/18" cleanly.
data class MeSemesterProgress(
    val semesterId: String,
    val code: String,
    val startDate: String,
    val endDate: String,
    val currentWeek: Int,
    val totalWeeks: Int,
    val progressPercent: Int,
)

// CR + class-hours rollup. `cr` is the weighted partial average across every
// StudentGrade row in the active semester; `crDelta` is signed vs. the most
// recent previous semester's same computation. `completedHours` /
// `totalHours` back the "créditos" chip (repurposed to current-semester hours).
data class MeEnrollmentSummary(
    val cr: Double?,
    val crDelta: Double?,
    val completedHours: Int,
    val totalHours: Int,
)

// Closest upcoming evaluation across any enrolled class. Mirrors
// `UpcomingEvaluationRow` with the date kept raw so the Swift layer handles
// locale-specific formatting.
data class MeNextExam(
    val date: String,
    val evaluationName: String?,
    val disciplineCode: String,
    val disciplineName: String,
)
