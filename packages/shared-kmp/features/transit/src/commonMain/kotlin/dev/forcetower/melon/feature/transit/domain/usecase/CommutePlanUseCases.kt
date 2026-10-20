package dev.forcetower.melon.feature.transit.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.transit.data.CommutePlanRepository
import dev.forcetower.melon.feature.transit.domain.model.CommutePlan
import dev.forcetower.melon.feature.transit.domain.model.TransitError
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

// The mirrored plan — replays on subscription and after every write. An
// empty plan means the student hasn't set Meu trajeto up (or removed it).
@Inject
class ObserveCommutePlanUseCase internal constructor(
    private val repository: CommutePlanRepository,
) {
    operator fun invoke(): Flow<CommutePlan> = repository.observe()
}

// Pulls the server's plan into the mirror (another device may have changed
// it). The answer lands through `ObserveCommutePlanUseCase`; a failure keeps
// the mirrored plan.
@Inject
class RefreshCommutePlanUseCase internal constructor(
    private val repository: CommutePlanRepository,
) {
    suspend operator fun invoke(): Outcome<Unit, TransitError> = repository.refresh()
}

// Replaces the plan whole and answers with what the server resolved (stop
// names included), which is what the mirror then holds.
@Inject
class SaveCommutePlanUseCase internal constructor(
    private val repository: CommutePlanRepository,
) {
    suspend operator fun invoke(plan: CommutePlan): Outcome<CommutePlan, TransitError> = repository.save(plan)
}

@Inject
class RemoveCommutePlanUseCase internal constructor(
    private val repository: CommutePlanRepository,
) {
    suspend operator fun invoke(): Outcome<Unit, TransitError> = repository.remove()
}

// Logout teardown: drops the mirrored plan so it never leaks into the next
// account on the device.
@Inject
class ClearCommutePlanUseCase internal constructor(
    private val repository: CommutePlanRepository,
) {
    suspend operator fun invoke() = repository.clear()
}
