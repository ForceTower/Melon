package dev.forcetower.melon.feature.sync.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.SemesterSummary
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.core.sync.domain.repository.MirrorRepository
import dev.zacsweers.metro.Inject

// Pulls the lightweight semester list and returns the summaries to the caller.
// Used by SyncView to pick the active semester for the focused payload pull.
// Larger orchestrations (refresh, full sync) drive the same repo method
// without needing the summaries returned.
@Inject
class SyncSemesterListUseCase internal constructor(
    private val mirror: MirrorRepository,
) {
    suspend operator fun invoke(): Outcome<List<SemesterSummary>, SyncError> =
        mirror.syncSemesterList()
}
