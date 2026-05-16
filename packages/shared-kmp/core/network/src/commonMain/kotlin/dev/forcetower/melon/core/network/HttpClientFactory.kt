package dev.forcetower.melon.core.network

import co.touchlab.kermit.Logger as KermitLogger
import co.touchlab.kermit.Severity
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger as KtorLogger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val AuthInterceptor = createClientPlugin("AuthInterceptor", ::AuthInterceptorConfig) {
    val source = pluginConfig.authTokenSource
        ?: error("AuthInterceptor requires an AuthTokenSource")
    onRequest { request, _ ->
        source.getAccessToken()?.let { token ->
            request.headers { append(HttpHeaders.Authorization, "Bearer $token") }
        }
    }
}

internal class AuthInterceptorConfig {
    var authTokenSource: AuthTokenSource? = null
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
            scoped.log(severity = Severity.Info, tag = "ktor", throwable = null, message = message)
        }
    }
}
