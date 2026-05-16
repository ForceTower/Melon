package dev.forcetower.melon.feature.sync.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.core.sync.domain.repository.MirrorRepository
import dev.zacsweers.metro.Inject

// Pull-to-refresh / targeted pull: fetch one semester's full payload and apply
// it to the local mirror. Pure force — ignores throttles. Use the
// RefreshActiveSemesters use case for the cadence-gated path.
@Inject
class SyncSemesterUseCase internal constructor(
    private val mirror: MirrorRepository,
) {
    suspend operator fun invoke(semesterId: String): Outcome<Unit, SyncError> =
        mirror.syncSemester(semesterId)
}
