package dev.forcetower.melon.core.network

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import dev.forcetower.melon.core.logging.LoggingConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// LogWriter that ships log records to the Melon API's /api/logs endpoint,
// which proxies the records to the OTel backend server-side. Runs a
// dedicated drain coroutine that batches records by count or time, whichever
// comes first. Backpressure strategy is "drop newest" — logging must never
// block the caller or cascade into the caller's scope.
//
// Uses its own HttpClient — NOT the shared one from NetworkGraph — because
// that client installs Ktor's Logging plugin, which would feed every outbound
// request back into this writer and spiral. The client here carries only the
// two plugins we actually need: ContentNegotiation (JSON body) and
// MachineIdInterceptor (so the server can attribute records to a device).
class ApiLogWriter internal constructor(
    private val scope: CoroutineScope,
    private val http: HttpClient,
    private val endpoint: String,
    private val service: String,
    private val minSeverity: Severity,
    private val batchSize: Int,
    private val flushInterval: Duration,
    queueCapacity: Int,
) : LogWriter() {

    private val queue = Channel<ApiLogRecord>(capacity = queueCapacity)

    init {
        scope.launch { drain() }
    }

    override fun isLoggable(tag: String, severity: Severity): Boolean = severity >= minSeverity

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val attributes = buildMap {
            put("log.tag", tag)
            throwable?.let {
                put("exception.type", it::class.simpleName ?: "Throwable")
                it.message?.let { msg -> put("exception.message", msg) }
                put("exception.stacktrace", it.stackTraceToString())
            }
        }
        val record = ApiLogRecord(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            severity = severity.toApiSeverity(),
            message = message,
            attributes = attributes,
        )
        // trySend is non-blocking; if the queue is full we drop silently. A
        // blocked producer would be worse than a dropped log.
        queue.trySend(record).onFailure { /* dropped */ }
    }

    private suspend fun drain() {
        val batch = ArrayList<ApiLogRecord>(batchSize)
        while (scope.isActive) {
            try {
                batch += queue.receive()
                // Fill up without blocking.
                while (batch.size < batchSize) {
                    val next = queue.tryReceive().getOrNull() ?: break
                    batch += next
                }
                // If we still haven't filled the batch, wait up to flushInterval
                // for more records to arrive before shipping.
                while (batch.size < batchSize) {
                    val next = withTimeoutOrNull(flushInterval) { queue.receive() } ?: break
                    batch += next
                }
                postBatch(batch)
            } catch (_: ClosedReceiveChannelException) {
                break
            } catch (_: Throwable) {
                // Never surface transport failures — just drop the batch and keep draining.
            } finally {
                batch.clear()
            }
        }
    }

    private suspend fun postBatch(records: List<ApiLogRecord>) {
        if (records.isEmpty()) return
        val response = http.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(ApiLogsRequest(service = service, records = records))
        }
        if (!response.status.isSuccess()) {
            // Swallow body to release the connection; we don't log the failure
            // (that would recurse through this writer).
            response.bodyAsText()
        }
    }

    companion object {
        fun create(
            scope: CoroutineScope,
            http: HttpClient,
            config: LoggingConfig,
            batchSize: Int = 50,
            flushInterval: Duration = 5.seconds,
            queueCapacity: Int = 1024,
        ): ApiLogWriter {
            val baseUrl = requireNotNull(config.apiBaseUrl) {
                "LoggingConfig.apiBaseUrl is null — gate on enableRemote && apiBaseUrl before calling"
            }.trimEnd('/')
            return ApiLogWriter(
                scope = scope,
                http = http,
                endpoint = "$baseUrl/api/logs",
                service = config.serviceName,
                minSeverity = config.minRemoteSeverity,
                batchSize = batchSize,
                flushInterval = flushInterval,
                queueCapacity = queueCapacity,
            )
        }

        fun buildHttpClient(
            engine: HttpClientEngine,
            json: Json,
            machineIdSource: MachineIdSource,
        ): HttpClient = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(MachineIdInterceptor) { this.machineIdSource = machineIdSource }
        }
    }
}

@Serializable
internal data class ApiLogsRequest(
    val service: String,
    val records: List<ApiLogRecord>,
)

@Serializable
internal data class ApiLogRecord(
    val timestamp: Long,
    val severity: String,
    val message: String,
    val attributes: Map<String, String> = emptyMap(),
)

private fun Severity.toApiSeverity(): String = when (this) {
    Severity.Verbose -> "trace"
    Severity.Debug -> "debug"
    Severity.Info -> "info"
    Severity.Warn -> "warn"
    Severity.Error -> "error"
    Severity.Assert -> "fatal"
}
