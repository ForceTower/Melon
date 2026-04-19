package dev.forcetower.melon.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json

@ContributesTo(AppScope::class)
interface NetworkGraph {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun json(): Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

        @Provides
        @SingleIn(AppScope::class)
        fun httpClient(
            engine: HttpClientEngine,
            baseUrl: BaseUrl,
            authTokenSource: AuthTokenSource,
            json: Json,
        ): HttpClient = buildHttpClient(engine, baseUrl, authTokenSource, json)
    }
}
