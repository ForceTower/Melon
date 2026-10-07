package dev.forcetower.melon.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlinx.coroutines.test.runTest

class TlsInterceptionMonitorTest {
    private val monitor = TlsInterceptionMonitor()

    // Stands in for the platform trust manager: records what the probe saw,
    // then fails the handshake on hosts the network intercepts.
    private fun client(
        interceptedHost: String,
        reason: TlsFailureReason = TlsFailureReason.Intercepted,
        intercepting: () -> Boolean,
    ) = HttpClient(
        MockEngine { request ->
            if (request.url.host == interceptedHost && intercepting()) {
                TlsDiagnosticReporter.record(
                    TlsDiagnostic(
                        host = interceptedHost,
                        capturedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                        reason = reason,
                        issuerCommonName = "FortiGate CA",
                        issuerOrganization = "Fortinet Inc.",
                        notBeforeEpochSeconds = null,
                        notAfterEpochSeconds = null,
                    ),
                )
                error("Trust anchor for certification path not found.")
            }
            respondOk()
        },
    ) {
        expectSuccess = false
        installTlsDiagnostics(monitor)
    }

    @Test
    fun interceptedHandshakeIsReportedWithTheProductName() =
        runTest {
            val http = client(API_HOST) { true }

            assertFailsWith<NetworkError.Tls.Intercepted> { http.get("https://$API_HOST/api/sync/profile") }

            assertEquals(TlsInterception(API_HOST, "Fortinet"), monitor.interception.value)
        }

    @Test
    fun aResponseFromTheInterceptedHostClearsIt() =
        runTest {
            var intercepting = true
            val http = client(API_HOST) { intercepting }
            assertFailsWith<NetworkError.Tls.Intercepted> { http.get("https://$API_HOST/api/sync/profile") }

            intercepting = false
            http.get("https://$API_HOST/api/sync/profile")

            assertNull(monitor.interception.value)
        }

    @Test
    fun aResponseFromAnotherHostKeepsIt() =
        runTest {
            val http = client(API_HOST) { true }
            assertFailsWith<NetworkError.Tls.Intercepted> { http.get("https://$API_HOST/api/sync/profile") }

            http.get("https://academico.uefs.br/")

            assertEquals(API_HOST, monitor.interception.value?.host)
        }

    @Test
    fun otherTlsFailuresAreNotReported() =
        runTest {
            val http = client(API_HOST, reason = TlsFailureReason.ClockSkew) { true }

            assertFailsWith<NetworkError.Tls.ClockSkew> { http.get("https://$API_HOST/api/sync/profile") }

            assertNull(monitor.interception.value)
        }

    private companion object {
        const val API_HOST = "melon.forcetower.dev"
    }
}
