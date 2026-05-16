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
