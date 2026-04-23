package dev.forcetower.melon.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
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
import kotlinx.datetime.Clock

// LogWriter that ships log records to an OTLP/HTTP (JSON) collector. Runs a
// dedicated drain coroutine that batches records by count or time, whichever
// comes first. Backpressure strategy is "drop newest" — logging must never
// block the caller or cascade into the caller's scope.
class OtlpLogWriter internal constructor(
    private val scope: CoroutineScope,
    private val http: HttpClient,
    private val endpoint: String,
    private val extraHeaders: Map<String, String>,
    private val resourceAttributes: List<OtlpKeyValue>,
    private val minSeverity: Severity,
    private val batchSize: Int,
    private val flushInterval: Duration,
    queueCapacity: Int,
) : LogWriter() {

    private val queue = Channel<OtlpLogRecord>(capacity = queueCapacity)

    init {
        scope.launch { drain() }
    }

    override fun isLoggable(tag: String, severity: Severity): Boolean = severity >= minSeverity

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val record = OtlpLogRecord(
            timeUnixNano = Clock.System.now().nanosecondsSinceEpoch().toString(),
            severityNumber = severity.toOtelSeverityNumber(),
            severityText = severity.name.uppercase(),
            body = stringValue(message),
            attributes = buildList {
                add(OtlpKeyValue("log.tag", stringValue(tag)))
                throwable?.let {
                    add(OtlpKeyValue("exception.type", stringValue(it::class.simpleName ?: "Throwable")))
                    it.message?.let { msg -> add(OtlpKeyValue("exception.message", stringValue(msg))) }
                    add(OtlpKeyValue("exception.stacktrace", stringValue(it.stackTraceToString())))
                }
            },
        )
        // trySend is non-blocking; if the queue is full we drop silently. A
        // blocked producer would be worse than a dropped log.
        queue.trySend(record).onFailure { /* dropped */ }
    }

    private suspend fun drain() {
        val batch = ArrayList<OtlpLogRecord>(batchSize)
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

    private suspend fun postBatch(records: List<OtlpLogRecord>) {
        if (records.isEmpty()) return
        val payload = OtlpLogsRequest(
            resourceLogs = listOf(
                OtlpResourceLogs(
                    resource = OtlpResource(resourceAttributes),
                    scopeLogs = listOf(
                        OtlpScopeLogs(
                            scope = OtlpScope(name = "co.touchlab.kermit", version = "2.0.4"),
                            logRecords = records,
                        ),
                    ),
                ),
            ),
        )
        val response = http.post(endpoint) {
            contentType(ContentType.Application.Json)
            for ((name, value) in extraHeaders) header(name, value)
            setBody(payload)
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
        ): OtlpLogWriter {
            val endpoint = requireNotNull(config.otlpEndpoint) {
                "LoggingConfig.otlpEndpoint is null — gate on LoggingConfig.enableRemote before calling"
            }.trimEnd('/') + "/v1/logs"
            val resource = buildList {
                add(OtlpKeyValue("service.name", stringValue(config.serviceName)))
                kvString("service.version", config.serviceVersion)?.let(::add)
                kvString("deployment.environment", config.deploymentEnvironment)?.let(::add)
            }
            return OtlpLogWriter(
                scope = scope,
                http = http,
                endpoint = endpoint,
                extraHeaders = config.otlpHeaders,
                resourceAttributes = resource,
                minSeverity = config.minRemoteSeverity,
                batchSize = batchSize,
                flushInterval = flushInterval,
                queueCapacity = queueCapacity,
            )
        }

        fun buildHttpClient(
            engine: io.ktor.client.engine.HttpClientEngine,
            json: kotlinx.serialization.json.Json,
        ): HttpClient = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
        }
    }
}

private fun Severity.toOtelSeverityNumber(): Int = when (this) {
    Severity.Verbose -> 1   // TRACE
    Severity.Debug -> 5     // DEBUG
    Severity.Info -> 9      // INFO
    Severity.Warn -> 13     // WARN
    Severity.Error -> 17    // ERROR
    Severity.Assert -> 21   // FATAL
}

private fun kotlinx.datetime.Instant.nanosecondsSinceEpoch(): Long =
    epochSeconds * 1_000_000_000L + nanosecondsOfSecond
