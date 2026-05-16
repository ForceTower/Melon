package dev.forcetower.melon.core.logging

import co.touchlab.kermit.Severity

data class LoggingConfig(
    val serviceName: String = "melon",
    val serviceVersion: String? = null,
    val deploymentEnvironment: String? = null,
    // Base URL of the Melon API. Remote log shipping POSTs to
    // "$apiBaseUrl/api/logs", which proxies records to the OTel backend.
    // UmbrellaGraph populates this from UmbrellaConfig.baseUrl — consumers of
    // LoggingConfig directly (tests) leave it null to disable remote shipping.
    val apiBaseUrl: String? = null,
    val minLocalSeverity: Severity = Severity.Verbose,
    val minRemoteSeverity: Severity = Severity.Info,
    val minCrashBreadcrumbSeverity: Severity = Severity.Info,
    val minCrashReportSeverity: Severity = Severity.Warn,
    val enableRemote: Boolean = true,
    val enableCrashReporting: Boolean = true,
)
