package dev.forcetower.melon.core.logging

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.OSLogWriter
import co.touchlab.kermit.crashlytics.CrashlyticsLogWriter

@OptIn(ExperimentalKermitApi::class)
internal actual fun platformDefaultLogWriters(config: LoggingConfig): List<LogWriter> = buildList {
    // OSLogWriter writes everything it receives; the Logger enforces
    // minLocalSeverity (see LoggingGraph).
    add(OSLogWriter())
    if (config.enableCrashReporting) {
        add(
            CrashlyticsLogWriter(
                minSeverity = config.minCrashBreadcrumbSeverity,
                minCrashSeverity = config.minCrashReportSeverity,
            ),
        )
    }
}
