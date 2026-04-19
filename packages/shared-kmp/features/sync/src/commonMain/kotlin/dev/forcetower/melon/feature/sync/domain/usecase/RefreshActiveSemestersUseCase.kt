package dev.forcetower.melon.feature.sync.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.core.sync.domain.repository.MirrorRepository
import dev.forcetower.melon.core.sync.domain.repository.SyncStateRepository
import dev.zacsweers.metro.Inject
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val ONE_HOUR_MILLIS = 60L * 60L * 1000L

// Client-side freshness gate. If the 1h throttle has elapsed since the last
// active-semester pull, refreshes the list + every semester currently in
// range. Pull-to-refresh bypasses this via SyncSemesterUseCase directly.
@Inject
class RefreshActiveSemestersUseCase internal constructor(
    private val mirror: MirrorRepository,
    private val syncState: SyncStateRepository,
) {
    suspend operator fun invoke(force: Boolean = false): Outcome<Unit, SyncError> {
        val now = Clock.System.now()
        val nowEpoch = now.toEpochMilliseconds()
        if (!force) {
            val last = syncState.getLastActiveSemesterPulledAt()
            if (last != null && nowEpoch - last < ONE_HOUR_MILLIS) return Outcome.Ok(Unit)
        }

        val listOutcome = mirror.syncSemesterList()
        val summaries = when (listOutcome) {
            is Outcome.Err -> return listOutcome
            is Outcome.Ok -> listOutcome.value
        }

        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val active = summaries.filter { inRange(today, it.startDate, it.endDate) }

        for (summary in active) {
            when (val result = mirror.syncSemester(summary.id)) {
                is Outcome.Err -> return result
                is Outcome.Ok -> Unit
            }
        }

        syncState.setLastActiveSemesterPulledAt(nowEpoch)
        return Outcome.Ok(Unit)
    }

    private fun inRange(today: LocalDate, startIso: String, endIso: String): Boolean {
        val start = runCatching { LocalDate.parse(startIso) }.getOrNull() ?: return false
        val end = runCatching { LocalDate.parse(endIso) }.getOrNull() ?: return false
        return today in start..end
    }
}
