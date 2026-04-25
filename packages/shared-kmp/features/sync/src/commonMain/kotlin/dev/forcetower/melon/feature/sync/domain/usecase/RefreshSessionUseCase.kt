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

// Per-session refresh fired on every authenticated shell entry (fresh launch
// or logout→login). Profile + first-page messages run unthrottled — they're
// cheap single requests, and profile is what unsticks the overview footer's
// "sincronizando" label by importing the server's lastSyncCompletedAt.
// Per-semester payload pulls stay behind the 1h throttle; pull-to-refresh
// bypasses via SyncSemesterUseCase directly.
//
// Profile + messages errors are logged and swallowed so a hiccup in one
// subsystem doesn't cancel the others. Per-semester payload errors still
// short-circuit because a partial tree is worse than none.
@Inject
class RefreshSessionUseCase internal constructor(
    private val mirror: MirrorRepository,
    private val syncState: SyncStateRepository,
    logger: Logger,
) {
    private val log = logger.withTag("RefreshSessionUseCase")

    suspend operator fun invoke(force: Boolean = false): Outcome<Unit, SyncError> {
        log.i { "refresh session start force=$force" }
        val now = Clock.System.now()
        val nowEpoch = now.toEpochMilliseconds()

        when (val result = mirror.syncProfile()) {
            is Outcome.Err -> log.w { "profile refresh failed (ignored) err=${result.error}" }
            is Outcome.Ok -> Unit
        }

        when (val result = mirror.syncMessages(since = null, cursor = null)) {
            is Outcome.Err -> log.w { "messages first-page refresh failed (ignored) err=${result.error}" }
            is Outcome.Ok -> log.d { "messages first-page ok applied=${result.value.appliedCount}" }
        }

        when (val result = mirror.syncCalendarEvents()) {
            is Outcome.Err -> log.w { "calendar refresh failed (ignored) err=${result.error}" }
            is Outcome.Ok -> log.d { "calendar refresh ok applied=${result.value}" }
        }

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
                log.d { "per-semester pull throttled lastPulledAt=$last deltaMs=${nowEpoch - last}" }
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
