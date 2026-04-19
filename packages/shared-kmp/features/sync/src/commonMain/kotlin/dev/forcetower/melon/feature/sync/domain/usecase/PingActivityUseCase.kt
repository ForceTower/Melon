package dev.forcetower.melon.feature.sync.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.core.sync.domain.repository.MirrorRepository
import dev.zacsweers.metro.Inject

// Bumps users.last_active_at server-side. Fire-and-forget; the caller doesn't
// block on the result. Lives next to the sync use cases because the iOS
// SyncView fires it during the auth step.
@Inject
class PingActivityUseCase internal constructor(
    private val mirror: MirrorRepository,
) {
    suspend operator fun invoke(): Outcome<Unit, SyncError> = mirror.pingActivity()
}
