package dev.forcetower.melon.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.OSLogWriter

internal actual fun platformDefaultLogWriters(config: LoggingConfig): List<LogWriter> {
    // OSLogWriter writes everything it receives; the Logger enforces
    // minLocalSeverity (see LoggingGraph). Crashlytics fan-out lives in
    // CrashReporterLogWriter, wired from the host via CrashReporter.
    return listOf(OSLogWriter())
}
