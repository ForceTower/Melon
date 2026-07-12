package dev.forcetower.melon.feature.auth.data.mapper

import dev.forcetower.melon.feature.auth.data.dto.PasskeyAssertionPayload
import dev.forcetower.melon.feature.auth.data.dto.PasskeyAttestationPayload
import dev.forcetower.melon.feature.auth.data.dto.PasskeyAuthOptionsResponse
import dev.forcetower.melon.feature.auth.data.dto.PasskeyCredentialDto
import dev.forcetower.melon.feature.auth.data.dto.PasskeyRegisterVerifyRequest
import dev.forcetower.melon.feature.auth.domain.model.PasskeyAllowedCredential
import dev.forcetower.melon.feature.auth.domain.model.PasskeyAssertion
import dev.forcetower.melon.feature.auth.domain.model.PasskeyAttestation
import dev.forcetower.melon.feature.auth.domain.model.PasskeyChallenge
import dev.forcetower.melon.feature.auth.domain.model.PasskeyCredential

internal fun PasskeyAuthOptionsResponse.toDomain(): PasskeyChallenge {
    return PasskeyChallenge(
        sessionId = sessionId,
        challenge = options.challenge,
        rpId = options.rpId,
        userVerification = options.userVerification,
        timeout = options.timeout,
        allowCredentials = options.allowCredentials.orEmpty().map {
            PasskeyAllowedCredential(
                id = it.id,
                type = it.type,
                transports = it.transports.orEmpty(),
            )
        },
    )
}

internal fun PasskeyAssertion.toDto(): PasskeyAssertionPayload {
    return PasskeyAssertionPayload(
        id = id,
        rawId = rawId,
        type = "public-key",
        authenticatorAttachment = authenticatorAttachment,
        clientDataJSON = clientDataJSON,
        authenticatorData = authenticatorData,
        signature = signature,
        userHandle = userHandle,
    )
}

internal fun PasskeyAttestation.toVerifyRequest(deviceName: String?): PasskeyRegisterVerifyRequest {
    return PasskeyRegisterVerifyRequest(
        response = PasskeyAttestationPayload(
            id = id,
            rawId = rawId,
            authenticatorAttachment = authenticatorAttachment,
            clientDataJSON = clientDataJSON,
            attestationObject = attestationObject,
        ),
        deviceName = deviceName,
    )
}

internal fun PasskeyCredentialDto.toDomain(): PasskeyCredential {
    return PasskeyCredential(
        id = id,
        deviceName = deviceName,
        isSynced = deviceType == "multiDevice",
        createdAt = createdAt,
    )
}
