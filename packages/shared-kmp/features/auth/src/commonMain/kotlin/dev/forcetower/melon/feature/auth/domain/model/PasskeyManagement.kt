package dev.forcetower.melon.feature.auth.domain.model

/**
 * WebAuthn creation options for a brand-new passkey, from
 * `POST api/passkey/register/options`. The server emits a full
 * `PublicKeyCredentialCreationOptionsJSON`; we carry it verbatim as
 * [requestJson] because the Android `CredentialManager` consumes exactly that
 * shape (challenge, rp, user handle, excludeCredentials, …). Reconstructing it
 * client-side would risk drifting from what the server signed, so we pass it
 * through untouched.
 */
data class PasskeyRegistrationOptions(
    val requestJson: String,
)

/**
 * Native-built attestation for a freshly minted credential, submitted to
 * `POST api/passkey/register/verify`. All binary fields are base64url-encoded
 * by the platform layer, which is what the server expects.
 */
data class PasskeyAttestation(
    val id: String,
    val rawId: String,
    val authenticatorAttachment: String?,
    val clientDataJSON: String,
    val attestationObject: String,
)

/**
 * A registered passkey, as listed by `GET api/passkey/credentials`.
 * [createdAt] stays an ISO-8601 string so the native layer can format it with
 * the device locale instead of a hardcoded formatter.
 */
data class PasskeyCredential(
    val id: String,
    val deviceName: String?,
    // `multiDevice` credentials roam through the platform password manager
    // (Google Password Manager); `singleDevice` ones stay bound to hardware.
    val isSynced: Boolean,
    val createdAt: String,
)

/**
 * Failure surface for passkey management calls. [NotFound] is the 404 a rename
 * or delete returns when the credential id doesn't belong to the caller (or is
 * already gone) — the server can't distinguish the two, and neither do we.
 */
sealed interface PasskeyError {
    data object NoConnection : PasskeyError
    data object NotFound : PasskeyError
    data class Server(val message: String?) : PasskeyError
    data object Unexpected : PasskeyError
}
