package dev.forcetower.melon.feature.sync.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.OnboardingStatus
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.core.sync.domain.repository.MirrorRepository
import dev.zacsweers.metro.Inject

// Polled by SyncView's retry gates while the server's backfill worker is still
// catching up. Cheap server call (single status snapshot, no heavy joins).
@Inject
class FetchOnboardingStatusUseCase internal constructor(
    private val mirror: MirrorRepository,
) {
    suspend operator fun invoke(): Outcome<OnboardingStatus, SyncError> =
        mirror.fetchOnboardingStatus()
}
