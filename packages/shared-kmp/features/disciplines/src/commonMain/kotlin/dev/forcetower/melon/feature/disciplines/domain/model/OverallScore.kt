package dev.forcetower.melon.feature.disciplines.domain.model

// The CR of one program the student has taken disciplines in. A program is
// identified by the semester track: `null` is the regular undergrad calendar
// (2024.1, 2024.2, …), anything else is a program that runs its own calendar
// — pós-graduação, EAD, and the year-long health-course cycles all show up
// as tracked semesters (22.1RUE, 19.1PGM, 2023M).
data class ProgramScore(
    val track: String?,
    val value: Double,
    // `value` minus the CR as it stood before this program's most recent
    // semester with closed grades. Null until two of its semesters closed.
    val delta: Double?,
    val semesterCount: Int,
    val disciplineCount: Int,
)

// Every program CR the student has, most recently active first, plus which
// one they are currently in. Programs without a single closed grade have no
// entry — a freshly started mestrado shows up in `currentTrack` before it
// shows up in `programs`.
data class OverallScore(
    val programs: List<ProgramScore>,
    val currentTrack: String?,
) {
    // The program to show when the UI has room for exactly one number.
    val current: ProgramScore?
        get() = programs.firstOrNull { it.track == currentTrack } ?: programs.firstOrNull()

    // False for the overwhelming majority of students, who only ever take
    // undergrad disciplines. Screens keep their single-number layout until
    // this flips.
    val isSplit: Boolean
        get() = programs.size > 1

    companion object {
        val Empty = OverallScore(programs = emptyList(), currentTrack = null)
    }
}
