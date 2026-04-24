package dev.forcetower.melon.core.network

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.logging.LoggingConfig
import dev.forcetower.melon.core.logging.RemoteLogWriters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
            machineIdSource: MachineIdSource,
            json: Json,
            logger: Logger,
        ): HttpClient = buildHttpClient(engine, baseUrl, authTokenSource, machineIdSource, json, logger)

        // Contributes the ApiLogWriter into Kermit's writer list — lives here
        // rather than in LoggingGraph because constructing the writer needs
        // MachineIdSource + MachineIdInterceptor, which are network concerns
        // that core/logging can't see without a dep cycle.
        @Provides
        @SingleIn(AppScope::class)
        fun remoteLogWriters(
            loggingConfig: LoggingConfig,
            engine: HttpClientEngine,
            machineIdSource: MachineIdSource,
            json: Json,
        ): RemoteLogWriters {
            if (!loggingConfig.enableRemote || loggingConfig.apiBaseUrl == null) {
                return RemoteLogWriters.Empty
            }
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val http = ApiLogWriter.buildHttpClient(engine, json, machineIdSource)
            return RemoteLogWriters(listOf(ApiLogWriter.create(scope, http, loggingConfig)))
        }
    }
}
