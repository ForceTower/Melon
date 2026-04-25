package dev.forcetower.melon.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

// Mirrors the filtering semantics that kermit-crashlytics' CrashlyticsLogWriter
// used: everything at/above `minSeverity` becomes a breadcrumb via
// CrashReporter.log; entries at/above `minCrashSeverity` are additionally
// recorded as a non-fatal. The actual Firebase call lives in the host app.
internal class CrashReporterLogWriter(
    private val reporter: CrashReporter,
    private val minSeverity: Severity,
    private val minCrashSeverity: Severity,
) : LogWriter() {
    override fun isLoggable(tag: String, severity: Severity): Boolean =
        severity >= minSeverity

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val line = "${severity.name}/$tag: $message"
        reporter.log(line)
        if (severity >= minCrashSeverity) {
            reporter.recordNonFatal(line, throwable)
        }
    }
}
