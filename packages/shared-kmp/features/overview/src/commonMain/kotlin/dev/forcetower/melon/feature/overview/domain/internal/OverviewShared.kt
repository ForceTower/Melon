package dev.forcetower.melon.feature.overview.domain.internal

import dev.forcetower.melon.core.database.entity.SemesterEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Emits now, then every `periodMs`. Used to combine with DB flows so derived
// state (startsInMinutes, class state, daysUntil) refreshes on a tick even
// when the DB hasn't changed.
internal fun ticker(periodMs: Long = 30_000): Flow<LocalDateTime> = flow {
    while (true) {
        emit(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
        delay(periodMs)
    }
}

// Picks the semester whose [startDate, endDate] contains `todayIso`, falling
// back to the most recently started one. Mirrors the rule SyncViewModel and
// GetReadyOverviewUseCase already use so "current semester" stays consistent.
internal fun pickActiveSemester(
    all: List<SemesterEntity>,
    todayIso: String,
): SemesterEntity? {
    if (all.isEmpty()) return null
    val active = all.firstOrNull { it.startDate <= todayIso && todayIso <= it.endDate }
    if (active != null) return active
    return all.maxByOrNull { it.startDate }
}
