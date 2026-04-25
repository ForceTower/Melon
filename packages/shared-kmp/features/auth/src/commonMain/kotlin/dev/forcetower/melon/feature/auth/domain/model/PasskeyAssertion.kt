package dev.forcetower.melon.feature.auth.domain.model

/**
 * Server-issued challenge + session token + (optional) credential allowlist
 * the platform authenticator must satisfy. Returned by `BeginPasskeyLoginUseCase`
 * and consumed by the native ASAuthorization / CredentialManager layer.
 */
data class PasskeyChallenge(
    val sessionId: String,
    val challenge: String,
    val rpId: String,
    val userVerification: String?,
    val timeout: Long?,
    val allowCredentials: List<PasskeyAllowedCredential>,
)

data class PasskeyAllowedCredential(
    val id: String,
    val type: String,
    val transports: List<String>,
)

/**
 * Native-built assertion payload submitted back to the server for verification.
 * All binary fields are base64url-encoded by the platform layer.
 */
data class PasskeyAssertion(
    val id: String,
    val rawId: String,
    val authenticatorAttachment: String?,
    val clientDataJSON: String,
    val authenticatorData: String,
    val signature: String,
    val userHandle: String?,
)
