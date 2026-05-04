@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.forcetower.melon.core.network

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlin.time.Clock
import platform.CoreFoundation.CFErrorGetCode
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.Foundation.*
import platform.Security.*

// errSec OSStatus values that map to "the device clock is wrong (or the cert really
// is expired)". We treat both the same way — the user-facing copy will suggest
// checking date/time first.
private const val ERR_SEC_CERTIFICATE_EXPIRED = -67818
private const val ERR_SEC_CERTIFICATE_NOT_VALID_YET = -67817

// Inspect a server-trust challenge for diagnostics ONLY. We deliberately do not
// return a credential or a cancellation disposition — the IosNetworkEngine block
// always responds with PerformDefaultHandling so the system makes the actual trust
// decision. SecTrustEvaluateWithError here is a side-effect: it tells us whether
// the chain would fail validation, and gives us a CFError code to classify
// expired-vs-untrusted-issuer. If our inspection logic were buggy, the only thing
// we could break is the diagnostic — production trust is the system's call.
internal fun inspectTlsChallengeForDiagnostic(challenge: NSURLAuthenticationChallenge) {
    val space = challenge.protectionSpace
    if (space.authenticationMethod != NSURLAuthenticationMethodServerTrust) return
    val trust: SecTrustRef = space.serverTrust ?: return

    memScoped {
        val errorRef = alloc<CFErrorRefVar>()
        val passed = SecTrustEvaluateWithError(trust, errorRef.ptr)
        if (passed) return@memScoped
        val errorCode = errorRef.value?.let { CFErrorGetCode(it).toInt() }
        recordDiagnostic(trust = trust, host = space.host, errorCode = errorCode)
        errorRef.value?.let { CFRelease(it) }
    }
}

private fun recordDiagnostic(trust: SecTrustRef, host: String, errorCode: Int?) {
    val reason = when (errorCode) {
        ERR_SEC_CERTIFICATE_EXPIRED, ERR_SEC_CERTIFICATE_NOT_VALID_YET -> TlsFailureReason.ClockSkew
        null -> TlsFailureReason.Generic
        else -> TlsFailureReason.Intercepted
    }
    val issuerSummary = readIssuerSummary(trust)
    TlsDiagnosticReporter.record(
        TlsDiagnostic(
            host = host,
            capturedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            reason = reason,
            // SecCertificateCopySubjectSummary returns a single human-readable line
            // like "FortiGate CA" — no separate CN/O fields without an ASN.1 parser.
            // The matcher works fine against either field, and UI displayName falls
            // through to whichever is non-null.
            issuerCommonName = issuerSummary,
            issuerOrganization = null,
            notBeforeEpochSeconds = null,
            notAfterEpochSeconds = null,
        ),
    )
}

@Suppress("DEPRECATION")
private fun readIssuerSummary(trust: SecTrustRef): String? {
    val count = SecTrustGetCertificateCount(trust)
    // Index 0 is the leaf (its own subject is the server, not the issuer).
    // Index 1 is the issuing CA cert — its subject IS the issuer of the leaf.
    if (count < 2) return null
    val issuerCert = SecTrustGetCertificateAtIndex(trust, 1) ?: return null
    val summary: CFStringRef = SecCertificateCopySubjectSummary(issuerCert) ?: return null
    return CFBridgingRelease(summary)?.toString()
}
