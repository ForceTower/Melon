package dev.forcetower.melon.feature.auth.domain.model

sealed interface LoginError {
    enum class Kind : LoginError {
        NoConnection,
        InvalidCredentials,
        Unexpected,
    }

    data class Server(val message: String?) : LoginError

    // The TLS chain was signed by an issuer the device doesn't trust — typically a
    // corporate proxy or local AV that re-signs HTTPS. issuerName is the friendliest
    // name we could pull off the chain (curated product name, otherwise issuer O,
    // otherwise issuer CN); null when nothing was extractable, in which case the UI
    // should fall back to a generic "your network is intercepting traffic" copy.
    data class TlsIntercepted(val issuerName: String?) : LoginError

    // The leaf certificate's validity window doesn't include the device clock.
    // Almost always the user's clock; very occasionally a genuinely expired cert.
    data object TlsClockSkew : LoginError

    // TLS handshake failed for a reason we couldn't classify (cipher mismatch,
    // protocol downgrade, malformed cert, etc.).
    data object TlsGeneric : LoginError
}
