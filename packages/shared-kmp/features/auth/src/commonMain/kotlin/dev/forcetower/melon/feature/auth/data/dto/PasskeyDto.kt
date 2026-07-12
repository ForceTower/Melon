package dev.forcetower.melon.feature.auth.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class PasskeyAuthOptionsRequest(
    val username: String? = null,
)

@Serializable
internal data class PasskeyAuthOptionsResponse(
    val options: PasskeyAuthOptions,
    val sessionId: String,
)

@Serializable
internal data class PasskeyAuthOptions(
    val challenge: String,
    val rpId: String,
    val timeout: Long? = null,
    val userVerification: String? = null,
    val allowCredentials: List<PasskeyAllowCredential>? = null,
)

@Serializable
internal data class PasskeyAllowCredential(
    val id: String,
    val type: String,
    val transports: List<String>? = null,
)

@Serializable
internal data class PasskeyAssertionPayload(
    val id: String,
    val rawId: String,
    val type: String,
    val authenticatorAttachment: String? = null,
    val clientDataJSON: String,
    val authenticatorData: String,
    val signature: String,
    val userHandle: String? = null,
)

@Serializable
internal data class PasskeyAuthVerifyRequest(
    val sessionId: String,
    val response: PasskeyAssertionPayload,
)

// ── Registration & management ──

// Flat wire format for `POST api/passkey/register/verify`; the server reshapes
// it into the nested W3C `RegistrationResponseJSON` before verifying.
@Serializable
internal data class PasskeyRegisterVerifyRequest(
    val response: PasskeyAttestationPayload,
    val deviceName: String? = null,
)

@Serializable
internal data class PasskeyAttestationPayload(
    val id: String,
    val rawId: String,
    val type: String = "public-key",
    val authenticatorAttachment: String? = null,
    val clientDataJSON: String,
    val attestationObject: String,
)

@Serializable
internal data class PasskeyCredentialsResponse(
    val credentials: List<PasskeyCredentialDto> = emptyList(),
)

@Serializable
internal data class PasskeyCredentialDto(
    val id: String,
    val deviceName: String? = null,
    val deviceType: String,
    val createdAt: String,
)

@Serializable
internal data class PasskeyRenameRequest(
    val deviceName: String,
)
