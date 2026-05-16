package dev.forcetower.melon.core.network

sealed class NetworkError(message: String?, cause: Throwable?) : Exception(message, cause) {
    sealed class Tls(message: String?, cause: Throwable?) : NetworkError(message, cause) {
        // The TLS chain was signed by an issuer that isn't trusted by the device — the
        // typical cause is a corporate proxy or local AV that re-signs traffic with its
        // own root CA. productName is the curated, user-friendly name (e.g. "Fortinet")
        // when the issuer matched a known product; otherwise null. The raw issuer fields
        // (CN/O) are always populated when available so the UI can name the issuer even
        // when no curated rule matched.
        class Intercepted(
            val productName: String?,
            val issuerCommonName: String?,
            val issuerOrganization: String?,
            cause: Throwable?,
        ) : Tls(
            message = "TLS chain signed by untrusted issuer" +
                (productName ?: issuerOrganization ?: issuerCommonName)
                    ?.let { " ($it)" }
                    .orEmpty(),
            cause = cause,
        ) {
            // Best display name for UI: curated product name → issuer organization → issuer CN.
            val displayName: String? get() = productName ?: issuerOrganization ?: issuerCommonName
        }

        // The leaf certificate's validity window doesn't include the device's current
        // clock. notBefore/notAfter are in epoch seconds; either may be null when the
        // engine couldn't extract the dates (iOS's first-cut path leaves them null).
        class ClockSkew(
            val deviceTimeEpochSeconds: Long,
            val notBeforeEpochSeconds: Long?,
            val notAfterEpochSeconds: Long?,
            cause: Throwable?,
        ) : Tls("TLS handshake failed: device clock outside certificate validity window", cause)

        // TLS handshake failed for a reason we couldn't classify (cipher mismatch,
        // protocol downgrade, malformed cert, etc.).
        class Generic(cause: Throwable?) : Tls("TLS handshake failed", cause)
    }

    class Other(cause: Throwable?) : NetworkError(cause?.message, cause)
}
