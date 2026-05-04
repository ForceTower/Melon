package dev.forcetower.melon.core.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpResponseValidator

// Translates engine-thrown TLS exceptions into the typed NetworkError.Tls.* family
// using diagnostics that the platform layer recorded just before the throw. If no
// diagnostic is present (non-TLS failure, or recorded for a different URL), the
// original exception flows through untouched.
internal fun HttpClientConfig<*>.installTlsDiagnostics() {
    HttpResponseValidator {
        handleResponseExceptionWithRequest { cause, request ->
            if (cause is NetworkError) return@handleResponseExceptionWithRequest
            val diagnostic = TlsDiagnosticReporter.consume(request.url.host)
            if (diagnostic != null) {
                throw diagnostic.toNetworkError(cause)
            }
        }
    }
}
