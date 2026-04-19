package dev.forcetower.melon.feature.sync.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.core.sync.domain.repository.MirrorRepository
import dev.zacsweers.metro.Inject

// Pulls the caller's user + student + course into local tables. Called at
// login and on demand (profile-changed pushes, "refresh my info" actions).
@Inject
class SyncProfileUseCase internal constructor(
    private val mirror: MirrorRepository,
) {
    suspend operator fun invoke(): Outcome<Unit, SyncError> = mirror.syncProfile()
}
