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
)

// Student-facing profile fields. `campus` is intentionally absent: every
// upstream today is UEFS, so the iOS mapping hardcodes it.
data class MeIdentity(
    val userName: String,
    val firstName: String,
    val courseName: String?,
    val enrollmentNumber: String,
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
