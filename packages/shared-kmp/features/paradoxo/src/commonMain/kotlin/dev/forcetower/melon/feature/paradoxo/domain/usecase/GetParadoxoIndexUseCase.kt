package dev.forcetower.melon.feature.paradoxo.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.paradoxo.data.network.ParadoxoService
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoError
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoIndexEntry
import dev.zacsweers.metro.Inject

// Full searchable catalogue of disciplines + teachers. Fetched once per
// screen session; the query filter runs client-side.
@Inject
class GetParadoxoIndexUseCase internal constructor(
    private val service: ParadoxoService,
) {
    suspend operator fun invoke(): Outcome<List<ParadoxoIndexEntry>, ParadoxoError> =
        service.index()
}
