package dev.forcetower.melon.feature.schedule.domain.internal

import dev.forcetower.melon.core.database.entity.SemesterEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

// Emits the current LocalDateTime, then re-emits every `periodMs`. Schedule
// only needs midnight-level granularity (for today-index rolls) so the default
// is coarser than Overview's 30s ticker. Mirrors the overview helper — kept
// local because the overview copy is `internal` to that module.
internal fun ticker(periodMs: Long = 60_000): Flow<LocalDateTime> = flow {
    while (true) {
        emit(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
        delay(periodMs)
    }
}

// Picks the semester whose [startDate, endDate] contains `todayIso`, falling
// back to the most recently started one. Mirrors the overview rule so "current
// semester" stays consistent across features.
internal fun pickActiveSemester(
    all: List<SemesterEntity>,
    todayIso: String,
): SemesterEntity? {
    if (all.isEmpty()) return null
    val active = all.firstOrNull { it.startDate <= todayIso && todayIso <= it.endDate }
    if (active != null) return active
    return all.maxByOrNull { it.startDate }
}
