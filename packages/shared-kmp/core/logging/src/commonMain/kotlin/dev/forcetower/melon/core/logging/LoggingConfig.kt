package dev.forcetower.melon.core.logging

import co.touchlab.kermit.Severity

data class LoggingConfig(
    val serviceName: String = "melon",
    val serviceVersion: String? = null,
    val deploymentEnvironment: String? = null,
    val otlpEndpoint: String? = "https://o.forcetower.dev",
    val otlpHeaders: Map<String, String> = emptyMap(),
    val minLocalSeverity: Severity = Severity.Verbose,
    val minRemoteSeverity: Severity = Severity.Info,
    val minCrashBreadcrumbSeverity: Severity = Severity.Info,
    val minCrashReportSeverity: Severity = Severity.Warn,
    val enableRemote: Boolean = true,
    val enableCrashReporting: Boolean = true,
)
