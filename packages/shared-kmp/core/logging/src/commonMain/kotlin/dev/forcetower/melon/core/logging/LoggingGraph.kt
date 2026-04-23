package dev.forcetower.melon.core.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.LoggerConfig
import co.touchlab.kermit.StaticConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

@ContributesTo(AppScope::class)
interface LoggingGraph {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun loggerConfig(config: LoggingConfig, engine: HttpClientEngine, json: Json): LoggerConfig {
            val writers = buildList {
                addAll(platformDefaultLogWriters(config))
                if (config.enableRemote && config.otlpEndpoint != null) {
                    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                    val http = OtlpLogWriter.buildHttpClient(engine, json)
                    add(OtlpLogWriter.create(scope, http, config))
                }
            }
            // The Logger short-circuits anything below its minSeverity before
            // asking writers — so it must be the floor of every writer's own
            // minimum, otherwise writers that want more-verbose logs never see
            // them. Each writer then does its own final filtering via
            // isLoggable/minSeverity.
            val floor = listOf(
                config.minLocalSeverity,
                config.minRemoteSeverity,
                config.minCrashBreadcrumbSeverity,
            ).min()
            return StaticConfig(minSeverity = floor, logWriterList = writers)
        }

        @Provides
        @SingleIn(AppScope::class)
        fun logger(loggerConfig: LoggerConfig): Logger = Logger(loggerConfig)
    }
}
