package dev.forcetower.melon.feature.overview.domain.model

// Profile-driven top strip. `avatarUrl` is nullable until sync fills it; iOS
// falls back to a generated initial.
data class OverviewHeader(
    val userName: String,
    val avatarUrl: String?,
)

// The "Now" card surfaces either a class in progress (within its window) or
// the next one up. `startsInMinutes` is signed: negative means the class is
// already running; zero/positive is time until it starts. Client formats.
data class OverviewNowClass(
    val offerId: String,
    val code: String,
    val title: String,
    val teacherName: String?,
    val roomLocation: String?,
    val startTime: String,
    val endTime: String?,
    val startsInMinutes: Int,
    val topic: String?,
    val isHappeningNow: Boolean,
)

enum class OverviewClassState { DONE, NOW, NEXT, LATER }

data class OverviewTodayItem(
    val classId: String,
    val offerId: String,
    val code: String,
    val title: String,
    val startTime: String,
    val endTime: String?,
    val roomLocation: String?,
    val topic: String?,
    val state: OverviewClassState,
)

// First class of tomorrow plus how many more follow it — powers the hero's
// "day concluded" state ("Amanhã · Qua, 12 mar" row). Null flow emission when
// tomorrow has no classes.
data class OverviewTomorrowPreview(
    val code: String,
    val title: String,
    val startTime: String,
    val roomLocation: String?,
    val extraCount: Int,
)

data class OverviewDiscipline(
    val disciplineId: String,
    // DisciplineOffer id — the scope the detail screen keys off. Carried
    // from the list so tapping a card can seed the detail screen directly.
    val offerId: String,
    val code: String,
    val title: String,
    // Pre-formatted display value. "—" when no grade has landed, formatted
    // "X,Y" once one has (partial or final). Keeps the UI layer free of
    // number-formatting decisions.
    val gradeLabel: String,
    val status: OverviewDisciplineStatus,
    val semesterCode: String,
)

enum class OverviewDisciplineStatus { PARCIAL, FINAL, APROVADO, REPROVADO }

// Tile payloads. Each emits on its own flow so the UI reacts independently
// instead of forcing one mega-aggregate to refresh together.

data class OverviewMessagesTile(
    val unreadCount: Int,
    val lastSender: String?,
    val lastPreview: String?,
    // ISO timestamp of the latest message — the UI renders the relative "2h"
    // chip from it. Null only when the inbox is empty.
    val lastTimestamp: String?,
)

data class OverviewNextTestTile(
    val label: String,
    val disciplineName: String,
    val date: String,
    val daysUntil: Int,
)

data class OverviewAttendanceTile(
    // 0..100, rounded. Null when the semester has no hours tracked yet.
    val percentage: Int?,
    val lastDays: List<Boolean>,
    val allowedAbsences: Int,
    val periodDays: Int,
)

// Coeficiente (CR) tile — same weighted average the "Eu" hero shows, paired
// with the delta vs. the previous downloaded semester. `crDelta` is null when
// there's no prior semester to compare against; `comparisonSemesterCode` is
// null in the same case. Emitted as null when the active semester has no
// graded rows yet, so the tile falls back to its "em breve" empty state.
data class OverviewGradeTile(
    val cr: Double,
    val crDelta: Double?,
    val comparisonSemesterCode: String?,
)
