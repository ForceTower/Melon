package dev.forcetower.melon.core.network

import java.security.cert.CertificateException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import kotlin.time.Clock
import okhttp3.Interceptor
import okhttp3.Response

// OkHttp interceptor that runs diagnostics AFTER a TLS failure has already happened.
// Production traffic uses OkHttp's default trust path — we never wrap or replace the
// system X509TrustManager. When the call throws an SSLException we open a separate,
// short-lived probe socket to the same host, capture the certificate chain it offers,
// and record a TlsDiagnostic. The probe trust manager is intentionally permissive —
// that is why it is private to the probe function, never returned, and never
// installed on the real OkHttpClient.
internal class TlsDiagnosticInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        try {
            return chain.proceed(request)
        } catch (cause: SSLException) {
            runCatching { captureDiagnostic(request.url.host, request.url.port, cause) }
            throw cause
        }
    }

    private fun captureDiagnostic(host: String, port: Int, cause: SSLException) {
        val chain = probeServerCertificateChain(host, port) ?: return
        val leaf = chain.firstOrNull() ?: return
        val issuerDn = leaf.issuerX500Principal.name
        TlsDiagnosticReporter.record(
            TlsDiagnostic(
                host = host,
                capturedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                reason = classify(cause),
                issuerCommonName = parseRdn(issuerDn, "CN"),
                issuerOrganization = parseRdn(issuerDn, "O"),
                notBeforeEpochSeconds = leaf.notBefore.time / 1000,
                notAfterEpochSeconds = leaf.notAfter.time / 1000,
            ),
        )
    }

    // Opens a one-shot SSL connection whose ONLY purpose is to read the server's
    // certificate chain. The probe trust manager records the chain and then refuses
    // (throws) so the connection never advances past the handshake. Hostname is the
    // SNI source. Caller times this out aggressively — the original request has
    // already failed, the user is waiting on an error.
    private fun probeServerCertificateChain(host: String, port: Int): Array<X509Certificate>? {
        var captured: Array<X509Certificate>? = null
        val probe = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                throw CertificateException("probe — never trusted")
            }
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                captured = chain
                throw CertificateException("probe — never trusted")
            }
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val ctx = SSLContext.getInstance("TLS").apply { init(null, arrayOf(probe), null) }
        try {
            (ctx.socketFactory.createSocket() as SSLSocket).use { socket ->
                socket.soTimeout = PROBE_TIMEOUT_MS
                socket.connect(java.net.InetSocketAddress(host, port), PROBE_TIMEOUT_MS)
                runCatching { socket.startHandshake() }
            }
        } catch (_: Throwable) {
            // Probe failures are expected (the probe TM throws by design) and any
            // network error — refusal, timeout, DNS — is benign at this point.
        }
        return captured
    }

    private fun classify(cause: Throwable): TlsFailureReason {
        var t: Throwable? = cause
        while (t != null) {
            when (t) {
                is CertificateExpiredException, is CertificateNotYetValidException -> return TlsFailureReason.ClockSkew
            }
            t = t.cause
        }
        val message = generateSequence(cause as Throwable?) { it.cause }
            .joinToString(" | ") { it.message.orEmpty() }
            .lowercase()
        if ("notafter" in message || "notbefore" in message ||
            "valid from" in message || "expired" in message || "not yet valid" in message
        ) {
            return TlsFailureReason.ClockSkew
        }
        if ("trust anchor" in message || "certification path" in message || "self-signed" in message) {
            return TlsFailureReason.Intercepted
        }
        return TlsFailureReason.Generic
    }

    // X500Principal.name is RFC 2253-style (e.g. "CN=FortiGate CA,O=Fortinet,C=US"),
    // with backslash-escaping for commas inside values. Small parser instead of a
    // full RDN library for one diagnostic call site.
    private fun parseRdn(dn: String, attr: String): String? {
        val parts = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < dn.length) {
            val c = dn[i]
            if (c == '\\' && i + 1 < dn.length) {
                sb.append(dn[i + 1])
                i += 2
            } else if (c == ',') {
                parts += sb.toString()
                sb.setLength(0)
                i++
            } else {
                sb.append(c)
                i++
            }
        }
        if (sb.isNotEmpty()) parts += sb.toString()
        val prefix = "$attr="
        return parts.map { it.trim() }.firstOrNull { it.startsWith(prefix, ignoreCase = true) }
            ?.removePrefix(prefix)
            ?.removePrefix(prefix.lowercase())
            ?.takeIf { it.isNotEmpty() }
    }

    private companion object {
        const val PROBE_TIMEOUT_MS = 3_000
    }
}
