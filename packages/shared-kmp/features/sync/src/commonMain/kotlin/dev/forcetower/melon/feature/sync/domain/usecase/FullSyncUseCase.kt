package dev.forcetower.melon.feature.sync.domain.usecase

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.core.sync.domain.repository.MirrorRepository
import dev.forcetower.melon.core.sync.domain.repository.SyncStateRepository
import dev.zacsweers.metro.Inject
import kotlinx.datetime.Clock

// Initial / onboarding pull: profile + full semester list + every semester's
// payload. Serial on purpose — server filters by the caller's student and
// backfill ordering matters less than bounded concurrency at this scale.
// Flips `onboardingComplete` on success so subsequent app launches take the
// incremental path via RefreshSessionUseCase.
@Inject
class FullSyncUseCase internal constructor(
    private val mirror: MirrorRepository,
    private val syncState: SyncStateRepository,
    logger: Logger,
) {
    private val log = logger.withTag("FullSyncUseCase")

    suspend operator fun invoke(): Outcome<Unit, SyncError> {
        log.i { "full sync start" }
        when (val result = mirror.syncProfile()) {
            is Outcome.Err -> {
                log.w { "full sync aborted at profile step err=${result.error}" }
                return result
            }
            is Outcome.Ok -> Unit
        }

        val summaries = when (val result = mirror.syncSemesterList()) {
            is Outcome.Err -> {
                log.w { "full sync aborted at semester list step err=${result.error}" }
                return result
            }
            is Outcome.Ok -> result.value
        }

        log.i { "full sync fetched semester list count=${summaries.size}" }

        for (summary in summaries) {
            when (val result = mirror.syncSemester(summary.id)) {
                is Outcome.Err -> {
                    log.w { "full sync failed on semester id=${summary.id} err=${result.error}" }
                    return result
                }
                is Outcome.Ok -> Unit
            }
        }

        syncState.setLastActiveSemesterPulledAt(Clock.System.now().toEpochMilliseconds())
        syncState.setOnboardingComplete(true)
        log.i { "full sync complete semesters=${summaries.size}" }
        return Outcome.Ok(Unit)
    }
}
