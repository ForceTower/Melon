package dev.forcetower.melon.feature.schedule.domain.model

// Aggregates everything the week-focused schedule UI renders: the active
// semester summary, the week frame, and Monday-anchored per-day buckets. A
// populated ScheduleWeek is emitted even when there is no active semester —
// `semesterId` simply goes null and the seven days come through empty so the
// view never has to special-case a null container.
data class ScheduleWeek(
    val semesterId: String?,
    val semesterCode: String?,
    val weekNumber: Int,
    val weekStartIso: String,
    val todayDayIndex: Int?,
    val days: List<ScheduleDay>,
)

// Mon(0)..Sun(6). `classes` is pre-sorted by startTime and only contains
// allocations that actually belong to this calendar day.
data class ScheduleDay(
    val dayIndex: Int,
    val dateIso: String,
    val classes: List<ScheduleClass>,
)

// First future day with at least one scheduled class for the active
// semester. Scans across week boundaries — populated even when the rest
// of the current week is empty (Friday with no weekend classes still
// reaches into next week's Monday/Tuesday allocations). `topic` on the
// inner `ScheduleClass` is always null here since lectures are recorded
// against an exact (classId, date) pair and the next class day usually
// hasn't been synced yet; widgets that surface it should treat topic as
// optional.
data class NextClassDay(
    val dateIso: String,
    val daysAway: Int,
    val first: ScheduleClass,
    // All classes for the day, sorted by startTime (`first` is also classes[0]).
    val classes: List<ScheduleClass>,
)

// A single allocation rendered on the day column. `topic` is resolved from
// ClassLecture for the matching (classId, dateIso); null when no lecture has
// been synced for that date yet.
data class ScheduleClass(
    val allocationId: String,
    val classId: String,
    val offerId: String,
    val code: String,
    val title: String,
    val startTime: String,
    val endTime: String?,
    val teacherName: String?,
    val modulo: String?,
    val room: String?,
    val campus: String?,
    val topic: String?,
)
