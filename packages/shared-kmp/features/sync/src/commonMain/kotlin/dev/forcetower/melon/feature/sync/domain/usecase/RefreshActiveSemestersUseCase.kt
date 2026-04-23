package dev.forcetower.melon.feature.sync.domain.usecase

import co.touchlab.kermit.Logger
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

// Client-side freshness gate. Always refreshes the semester list — it's a
// cheap call and we want the sidebar/picker to reflect the server's latest
// state on every app open. The 1h throttle gates only the expensive
// per-semester payload pulls; pull-to-refresh bypasses that via
// SyncSemesterUseCase directly.
@Inject
class RefreshActiveSemestersUseCase internal constructor(
    private val mirror: MirrorRepository,
    private val syncState: SyncStateRepository,
    logger: Logger,
) {
    private val log = logger.withTag("RefreshActiveSemestersUseCase")

    suspend operator fun invoke(force: Boolean = false): Outcome<Unit, SyncError> {
        log.i { "refresh active semesters start force=$force" }
        val now = Clock.System.now()
        val nowEpoch = now.toEpochMilliseconds()

        val listOutcome = mirror.syncSemesterList()
        val summaries = when (listOutcome) {
            is Outcome.Err -> {
                log.w { "refresh aborted at semester list err=${listOutcome.error}" }
                return listOutcome
            }
            is Outcome.Ok -> listOutcome.value
        }

        if (!force) {
            val last = syncState.getLastActiveSemesterPulledAt()
            if (last != null && nowEpoch - last < ONE_HOUR_MILLIS) {
                log.d { "refresh throttled lastPulledAt=$last deltaMs=${nowEpoch - last}" }
                return Outcome.Ok(Unit)
            }
        }

        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val active = summaries.filter { inRange(today, it.startDate, it.endDate) }
        log.i { "refresh active semesters total=${summaries.size} active=${active.size}" }

        for (summary in active) {
            when (val result = mirror.syncSemester(summary.id)) {
                is Outcome.Err -> {
                    log.w { "refresh failed on semester id=${summary.id} err=${result.error}" }
                    return result
                }
                is Outcome.Ok -> Unit
            }
        }

        syncState.setLastActiveSemesterPulledAt(nowEpoch)
        log.i { "refresh complete active=${active.size}" }
        return Outcome.Ok(Unit)
    }

    private fun inRange(today: LocalDate, startIso: String, endIso: String): Boolean {
        val start = runCatching { LocalDate.parse(startIso) }.getOrNull() ?: return false
        val end = runCatching { LocalDate.parse(endIso) }.getOrNull() ?: return false
        return today in start..end
    }
}
