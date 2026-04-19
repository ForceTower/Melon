package dev.forcetower.melon.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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

fun buildHttpClient(
    engine: HttpClientEngine,
    baseUrl: BaseUrl,
    authTokenSource: AuthTokenSource,
    json: Json,
): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) { json(json) }
    install(DefaultRequest) {
        url(baseUrl.value)
    }
    install(AuthInterceptor) {
        this.authTokenSource = authTokenSource
    }
}
