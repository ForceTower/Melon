package dev.forcetower.melon.feature.me.domain.model

/**
 * Health of the SAGRES credentials the *server* syncs with — distinct from the
 * Melon session. The app can be perfectly signed in while the stored portal
 * password has stopped working, in which case every screen keeps serving from
 * the mirror and nothing new ever arrives.
 */
enum class CredentialStatus {
    Ok,
    Invalid,

    /** Nothing on file (passkey-only accounts). Not surfaced to the user. */
    None,
}

data class CredentialHealth(
    val status: CredentialStatus,
    /** Shown read-only in the re-auth sheet; the account can't be changed. */
    val username: String?,
)

/** Why a re-auth submission failed, so the sheet can pick the right message. */
sealed interface ReauthError {
    /** The portal rejected the password. */
    data object InvalidPassword : ReauthError

    /** Upstream is down or unreachable — worth retrying, password untouched. */
    data object Unavailable : ReauthError

    data object NoConnection : ReauthError

    data class Server(val message: String?) : ReauthError
}
