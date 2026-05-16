@file:OptIn(ExperimentalAtomicApi::class)

package dev.forcetower.melon.core.network

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock

// What the platform engine recorded when a TLS handshake failed. Populated by the
// OkHttp trust manager (Android/JVM) or the Darwin challenge handler (iOS); consumed
// by TlsDiagnosticPlugin when a matching request later throws.
internal data class TlsDiagnostic(
    val host: String,
    val capturedAtEpochMs: Long,
    val reason: TlsFailureReason,
    val issuerCommonName: String?,
    val issuerOrganization: String?,
    val notBeforeEpochSeconds: Long?,
    val notAfterEpochSeconds: Long?,
)

internal enum class TlsFailureReason { Intercepted, ClockSkew, Generic }

// Diagnostics are recorded from a non-suspending platform callback (URLSession
// delegate / X509TrustManager) and read back when the resulting Ktor exception
// surfaces. We use a single atomic slot keyed by URL — TLS failures generally come
// in waves with the same cause, and we additionally gate by URL + a freshness window
// to keep mis-attribution unlikely.
internal object TlsDiagnosticReporter {
    // 5s is generous: the trust manager runs synchronously on the request's I/O
    // thread, so the exception surfaces within milliseconds. Anything older almost
    // certainly belongs to a different failure.
    private const val FRESHNESS_WINDOW_MS = 5_000L

    private val slot = AtomicReference<TlsDiagnostic?>(null)

    fun record(diagnostic: TlsDiagnostic) {
        slot.store(diagnostic)
    }

    fun consume(host: String, nowEpochMs: Long = Clock.System.now().toEpochMilliseconds()): TlsDiagnostic? {
        val current = slot.load() ?: return null
        if (!current.host.equals(host, ignoreCase = true)) return null
        if (nowEpochMs - current.capturedAtEpochMs > FRESHNESS_WINDOW_MS) {
            slot.compareAndSet(current, null)
            return null
        }
        return if (slot.compareAndSet(current, null)) current else null
    }
}

// Curated mapping from issuer-name fragments to a friendly product name. We do
// substring-contains, case-insensitive — issuer fields aren't standardized so a
// loose match is the right tool. The list is intentionally short: it only exists to
// polish the most common interceptors. Anything not on the list still surfaces with
// the raw issuer CN/O via NetworkError.Tls.Intercepted.displayName.
internal object MitmIssuerMatcher {
    private data class Rule(val product: String, val needles: List<String>)

    private val rules = listOf(
        Rule("Fortinet", listOf("Fortinet", "FortiGate", "FortiGuard")),
        Rule("Kaspersky", listOf("Kaspersky")),
        Rule("Bitdefender", listOf("Bitdefender")),
        Rule("ESET", listOf("ESET")),
        Rule("Avast", listOf("avast")),
        Rule("AVG", listOf("AVG ")),
        Rule("Sophos", listOf("Sophos")),
        Rule("Webroot", listOf("Webroot")),
        Rule("Cisco Umbrella", listOf("Cisco Umbrella")),
        Rule("Zscaler", listOf("Zscaler")),
        Rule("Blue Coat", listOf("Blue Coat", "BlueCoat", "Symantec ProxySG")),
        Rule("Check Point", listOf("Check Point")),
        Rule("McAfee", listOf("McAfee")),
        Rule("Charles Proxy", listOf("Charles Proxy")),
        Rule("mitmproxy", listOf("mitmproxy")),
        Rule("Squid Proxy", listOf("Squid Proxy")),
    )

    fun match(issuerCommonName: String?, issuerOrganization: String?): String? {
        val haystack = listOfNotNull(issuerCommonName, issuerOrganization).joinToString(" | ")
        if (haystack.isEmpty()) return null
        return rules.firstOrNull { rule ->
            rule.needles.any { needle -> haystack.contains(needle, ignoreCase = true) }
        }?.product
    }
}

internal fun TlsDiagnostic.toNetworkError(cause: Throwable): NetworkError = when (reason) {
    TlsFailureReason.Intercepted -> NetworkError.Tls.Intercepted(
        productName = MitmIssuerMatcher.match(issuerCommonName, issuerOrganization),
        issuerCommonName = issuerCommonName,
        issuerOrganization = issuerOrganization,
        cause = cause,
    )
    TlsFailureReason.ClockSkew -> NetworkError.Tls.ClockSkew(
        deviceTimeEpochSeconds = capturedAtEpochMs / 1000,
        notBeforeEpochSeconds = notBeforeEpochSeconds,
        notAfterEpochSeconds = notAfterEpochSeconds,
        cause = cause,
    )
    TlsFailureReason.Generic -> NetworkError.Tls.Generic(cause)
}
