package dev.forcetower.melon.feature.sync.domain.usecase

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
// incremental path via RefreshActiveSemestersUseCase.
@Inject
class FullSyncUseCase internal constructor(
    private val mirror: MirrorRepository,
    private val syncState: SyncStateRepository,
) {
    suspend operator fun invoke(): Outcome<Unit, SyncError> {
        when (val result = mirror.syncProfile()) {
            is Outcome.Err -> return result
            is Outcome.Ok -> Unit
        }

        val summaries = when (val result = mirror.syncSemesterList()) {
            is Outcome.Err -> return result
            is Outcome.Ok -> result.value
        }

        for (summary in summaries) {
            when (val result = mirror.syncSemester(summary.id)) {
                is Outcome.Err -> return result
                is Outcome.Ok -> Unit
            }
        }

        syncState.setLastActiveSemesterPulledAt(Clock.System.now().toEpochMilliseconds())
        syncState.setOnboardingComplete(true)
        return Outcome.Ok(Unit)
    }
}
