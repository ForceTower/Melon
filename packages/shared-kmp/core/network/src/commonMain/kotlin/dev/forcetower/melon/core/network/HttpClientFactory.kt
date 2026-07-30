package dev.forcetower.melon.core.network

import co.touchlab.kermit.Logger as KermitLogger
import co.touchlab.kermit.Severity
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger as KtorLogger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val AuthInterceptor = createClientPlugin("AuthInterceptor", ::AuthInterceptorConfig) {
    val source = pluginConfig.authTokenSource
        ?: error("AuthInterceptor requires an AuthTokenSource")
    val refresher = pluginConfig.tokenRefresher
        ?: error("AuthInterceptor requires a TokenRefresher")

    onRequest { request, _ ->
        // Skip when the retry below already attached a freshly rotated token.
        if (request.headers.contains(HttpHeaders.Authorization)) return@onRequest
        source.getAccessToken()?.let { token ->
            request.headers { append(HttpHeaders.Authorization, "Bearer $token") }
        }
    }

    // The API answers 401 for an expired token and an invalid one alike, so
    // attempting the refresh is the only way to tell them apart.
    on(Send) { request ->
        val call = proceed(request)
        if (call.response.status != HttpStatusCode.Unauthorized) return@on call
        val stale = request.headers[HttpHeaders.Authorization]
            ?.removePrefix("Bearer ")
            ?: return@on call
        if (!refresher.refresh(stale)) return@on call

        // Swap the spent bearer for the freshly rotated one and re-send.
        request.headers.remove(HttpHeaders.Authorization)
        source.getAccessToken()?.let { token ->
            request.headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
        proceed(request)
    }
}

internal class AuthInterceptorConfig {
    var authTokenSource: AuthTokenSource? = null
    var tokenRefresher: TokenRefresher? = null
}

internal val MachineIdInterceptor = createClientPlugin("MachineIdInterceptor", ::MachineIdInterceptorConfig) {
    val source = pluginConfig.machineIdSource
        ?: error("MachineIdInterceptor requires a MachineIdSource")
    onRequest { request, _ ->
        val machineId = source.getMachineId()
        request.headers { append("X-Machine-Id", machineId) }
    }
}

internal class MachineIdInterceptorConfig {
    var machineIdSource: MachineIdSource? = null
}

fun buildHttpClient(
    engine: HttpClientEngine,
    baseUrl: BaseUrl,
    authTokenSource: AuthTokenSource,
    tokenRefresher: TokenRefresher,
    machineIdSource: MachineIdSource,
    json: Json,
    logger: KermitLogger,
): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) { json(json) }
    install(Logging) {
        this.logger = logger.asKtorLogger()
        level = LogLevel.INFO
    }
    install(DefaultRequest) {
        url(baseUrl.value)
    }
    install(AuthInterceptor) {
        this.authTokenSource = authTokenSource
        this.tokenRefresher = tokenRefresher
    }
    install(MachineIdInterceptor) {
        this.machineIdSource = machineIdSource
    }
    installTlsDiagnostics()
}

private fun KermitLogger.asKtorLogger(): KtorLogger {
    val scoped = this.withTag("ktor")
    return object : KtorLogger {
        override fun log(message: String) {
            // Debug keeps request/response lines in logcat but below
            // minRemoteSeverity, so they never ship to OpenObserve.
            scoped.log(severity = Severity.Debug, tag = "ktor", throwable = null, message = message)
        }
    }
}
