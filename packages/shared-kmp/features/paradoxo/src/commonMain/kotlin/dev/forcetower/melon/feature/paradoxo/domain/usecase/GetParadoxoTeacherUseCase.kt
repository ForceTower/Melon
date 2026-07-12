package dev.forcetower.melon.feature.paradoxo.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.paradoxo.data.network.ParadoxoService
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoError
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoTeacherDetail
import dev.zacsweers.metro.Inject

@Inject
class GetParadoxoTeacherUseCase internal constructor(
    private val service: ParadoxoService,
) {
    suspend operator fun invoke(id: String): Outcome<ParadoxoTeacherDetail, ParadoxoError> =
        service.teacher(id)
}
