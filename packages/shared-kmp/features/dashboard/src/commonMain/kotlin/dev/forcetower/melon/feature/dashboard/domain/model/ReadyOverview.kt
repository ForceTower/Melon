package dev.forcetower.melon.feature.dashboard.domain.model

// Snapshot displayed on the Ready screen (end of onboarding). Everything
// is pre-formatted except the time-until-next-class, which iOS renders in the
// device's locale.
data class ReadyOverview(
    val semesterCode: String?,
    val classCount: Int,
    val totalCredits: Int,
    val nextClass: NextClassInfo?,
)

data class NextClassInfo(
    val disciplineName: String,
    val startTime: String,
    val endTime: String?,
    val spaceLocation: String?,
    val teacherName: String?,
    // Whole minutes between "now" and the class start, computed against the
    // device clock at snapshot time. Always non-negative; rolls to next week
    // if no allocation is left today.
    val startsInMinutes: Int,
)
