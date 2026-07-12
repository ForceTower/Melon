package dev.forcetower.melon.feature.auth.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.auth.domain.model.PasskeyAttestation
import dev.forcetower.melon.feature.auth.domain.model.PasskeyError
import dev.forcetower.melon.feature.auth.domain.repository.PasskeyRepository
import dev.zacsweers.metro.Inject

@Inject
class RegisterPasskeyUseCase internal constructor(
    private val repository: PasskeyRepository,
) {
    suspend operator fun invoke(
        attestation: PasskeyAttestation,
        deviceName: String? = null,
    ): Outcome<Unit, PasskeyError> = repository.register(attestation, deviceName)
}
