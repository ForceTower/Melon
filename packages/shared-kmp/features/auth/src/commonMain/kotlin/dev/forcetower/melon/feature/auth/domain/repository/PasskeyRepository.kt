package dev.forcetower.melon.feature.auth.domain.repository

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.auth.domain.model.PasskeyAttestation
import dev.forcetower.melon.feature.auth.domain.model.PasskeyCredential
import dev.forcetower.melon.feature.auth.domain.model.PasskeyError
import dev.forcetower.melon.feature.auth.domain.model.PasskeyRegistrationOptions

// Manages the signed-in account's passkeys against apps/api: lists them,
// fetches WebAuthn creation options, enrolls a freshly minted credential, and
// renames/revokes existing ones. Distinct from `AuthRepository`, which owns
// the pre-session login (assertion) flow.
internal interface PasskeyRepository {
    suspend fun registrationOptions(): Outcome<PasskeyRegistrationOptions, PasskeyError>

    suspend fun register(
        attestation: PasskeyAttestation,
        deviceName: String?,
    ): Outcome<Unit, PasskeyError>

    suspend fun list(): Outcome<List<PasskeyCredential>, PasskeyError>

    suspend fun rename(id: String, deviceName: String): Outcome<Unit, PasskeyError>

    suspend fun delete(id: String): Outcome<Unit, PasskeyError>
}
