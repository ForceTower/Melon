package dev.forcetower.melon.core.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpResponseValidator

// Translates engine-thrown TLS exceptions into the typed NetworkError.Tls.* family
// using diagnostics that the platform layer recorded just before the throw. If no
// diagnostic is present (non-TLS failure, or recorded for a different URL), the
// original exception flows through untouched.
internal fun HttpClientConfig<*>.installTlsDiagnostics(monitor: TlsInterceptionMonitor) {
    HttpResponseValidator {
        validateResponse { response -> monitor.clear(response.call.request.url.host) }
        handleResponseExceptionWithRequest { cause, request ->
            if (cause is NetworkError) return@handleResponseExceptionWithRequest
            val diagnostic = TlsDiagnosticReporter.consume(request.url.host)
            if (diagnostic != null) {
                val error = diagnostic.toNetworkError(cause)
                if (error is NetworkError.Tls.Intercepted) {
                    monitor.report(request.url.host, error.displayName)
                }
                throw error
            }
        }
    }
}
