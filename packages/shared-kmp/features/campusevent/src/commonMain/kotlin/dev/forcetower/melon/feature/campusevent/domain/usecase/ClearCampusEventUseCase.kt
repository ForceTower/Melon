package dev.forcetower.melon.feature.campusevent.domain.usecase

import dev.forcetower.melon.feature.campusevent.data.CampusEventRepository
import dev.zacsweers.metro.Inject

// Logout teardown: drops the persisted featured-event snapshot so it never
// leaks into the next account on the device (iOS wipes it with the mirror).
@Inject
class ClearCampusEventUseCase internal constructor(
    private val repository: CampusEventRepository,
) {
    suspend operator fun invoke() = repository.clear()
}
