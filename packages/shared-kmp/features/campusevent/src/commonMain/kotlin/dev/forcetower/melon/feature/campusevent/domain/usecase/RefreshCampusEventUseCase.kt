package dev.forcetower.melon.feature.campusevent.domain.usecase

import dev.forcetower.melon.feature.campusevent.data.CampusEventRepository
import dev.zacsweers.metro.Inject

// Silent refresh: the result lands through `ObserveCampusEventUseCase`;
// failures keep the stale offline payload, mirroring iOS.
@Inject
class RefreshCampusEventUseCase internal constructor(
    private val repository: CampusEventRepository,
) {
    suspend operator fun invoke() = repository.refresh()
}
