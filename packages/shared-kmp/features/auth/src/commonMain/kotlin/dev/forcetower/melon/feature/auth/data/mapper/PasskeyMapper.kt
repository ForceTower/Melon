package dev.forcetower.melon.feature.auth.data.mapper

import dev.forcetower.melon.feature.auth.data.dto.PasskeyAssertionPayload
import dev.forcetower.melon.feature.auth.data.dto.PasskeyAuthOptionsResponse
import dev.forcetower.melon.feature.auth.domain.model.PasskeyAllowedCredential
import dev.forcetower.melon.feature.auth.domain.model.PasskeyAssertion
import dev.forcetower.melon.feature.auth.domain.model.PasskeyChallenge

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
