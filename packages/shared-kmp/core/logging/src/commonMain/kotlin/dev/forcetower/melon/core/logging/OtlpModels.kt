package dev.forcetower.melon.core.logging

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// OTLP/HTTP JSON envelope for logs. Subset sufficient for what we emit —
// we never receive these, so missing fields in the spec are fine to omit.
// https://opentelemetry.io/docs/specs/otlp/#otlphttp

@Serializable
internal data class OtlpLogsRequest(
    val resourceLogs: List<OtlpResourceLogs>,
)

@Serializable
internal data class OtlpResourceLogs(
    val resource: OtlpResource,
    val scopeLogs: List<OtlpScopeLogs>,
)

@Serializable
internal data class OtlpResource(
    val attributes: List<OtlpKeyValue>,
)

@Serializable
internal data class OtlpScopeLogs(
    val scope: OtlpScope,
    val logRecords: List<OtlpLogRecord>,
)

@Serializable
internal data class OtlpScope(
    val name: String,
    val version: String? = null,
)

@Serializable
internal data class OtlpLogRecord(
    val timeUnixNano: String,
    val severityNumber: Int,
    val severityText: String,
    val body: OtlpAnyValue,
    val attributes: List<OtlpKeyValue> = emptyList(),
)

@Serializable
internal data class OtlpKeyValue(
    val key: String,
    val value: OtlpAnyValue,
)

@Serializable
internal data class OtlpAnyValue(
    @SerialName("stringValue") val stringValue: String? = null,
    @SerialName("intValue") val intValue: Long? = null,
    @SerialName("boolValue") val boolValue: Boolean? = null,
)

internal fun stringValue(v: String) = OtlpAnyValue(stringValue = v)

internal fun kvString(key: String, value: String?) =
    value?.let { OtlpKeyValue(key, stringValue(it)) }
