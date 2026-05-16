package dev.forcetower.unes.ui.feature.schedule

import androidx.compose.ui.graphics.Color

// Local UI projection of a schedule entry — mirrors iOS
// `apps/ios/UNES/Features/Schedule/Models/ScheduleModels.swift` and the
// JSX prototype `unes/project/screens-schedule.jsx`. The KMP-backed view
// model isn't wired yet; until it is, the screen renders fixtures from
// `ScheduleFixtures` so the JSX prototype, iOS, and Android stay aligned.
internal data class ScheduleClass(
    val start: String,
    val end: String,
    val code: String,
    val title: String,
    val prof: String,
    val color: Color,
    val modulo: String?,
    val room: String?,
    val campus: String?,
    val topic: String?,
    // DisciplineOffer id — non-null once the KMP feed lands. Pre-sync /
    // fixture rows leave it null so the row renders non-tappable (mirrors
    // iOS `DayColumn.detailSeed`).
    val offerId: String? = null,
)

internal enum class ScheduleClassState { Done, Now, Next, Later, Future }

internal val ScheduleClass.startMin: Int get() = scheduleToMin(start)
internal val ScheduleClass.endMin: Int get() = scheduleToMin(end)
internal val ScheduleClass.durationMin: Int get() = endMin - startMin

internal fun scheduleToMin(t: String): Int {
    val parts = t.split(":")
    if (parts.size != 2) return 0
    val h = parts[0].toIntOrNull() ?: 0
    val m = parts[1].toIntOrNull() ?: 0
    return h * 60 + m
}

internal fun scheduleStateFor(
    cls: ScheduleClass,
    isToday: Boolean,
    nowMin: Int,
): ScheduleClassState {
    if (!isToday) return ScheduleClassState.Future
    if (nowMin >= cls.endMin) return ScheduleClassState.Done
    if (nowMin >= cls.startMin) return ScheduleClassState.Now
    if (cls.startMin - nowMin < 60) return ScheduleClassState.Next
    return ScheduleClassState.Later
}
