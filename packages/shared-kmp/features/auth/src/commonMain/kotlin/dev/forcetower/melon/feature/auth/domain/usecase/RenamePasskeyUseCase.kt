package dev.forcetower.melon.feature.auth.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.auth.domain.model.PasskeyError
import dev.forcetower.melon.feature.auth.domain.repository.PasskeyRepository
import dev.zacsweers.metro.Inject

@Inject
class RenamePasskeyUseCase internal constructor(
    private val repository: PasskeyRepository,
) {
    suspend operator fun invoke(id: String, deviceName: String): Outcome<Unit, PasskeyError> =
        repository.rename(id, deviceName.trim())
}
