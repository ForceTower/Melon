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

// A single allocation rendered on the day column. `topic` is resolved from
// ClassLecture for the matching (classId, dateIso); null when no lecture has
// been synced for that date yet.
data class ScheduleClass(
    val allocationId: String,
    val classId: String,
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
