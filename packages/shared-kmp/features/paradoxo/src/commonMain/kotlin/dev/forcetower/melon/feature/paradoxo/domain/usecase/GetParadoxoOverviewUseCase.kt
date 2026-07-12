package dev.forcetower.melon.feature.paradoxo.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.paradoxo.data.network.ParadoxoService
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoError
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoOverview
import dev.zacsweers.metro.Inject

// Home payload: curated pulse facts, the student's own disciplines with
// historical means, the explore rankings, and the footer totals.
@Inject
class GetParadoxoOverviewUseCase internal constructor(
    private val service: ParadoxoService,
) {
    suspend operator fun invoke(): Outcome<ParadoxoOverview, ParadoxoError> = service.overview()
}
