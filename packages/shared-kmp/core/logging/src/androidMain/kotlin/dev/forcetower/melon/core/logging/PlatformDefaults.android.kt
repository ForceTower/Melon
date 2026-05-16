package dev.forcetower.melon.core.logging

import co.touchlab.kermit.LogWriter

internal actual fun platformDefaultLogWriters(config: LoggingConfig): List<LogWriter> {
    // The Logger itself enforces minLocalSeverity (see LoggingGraph). Timber
    // trees are planted by the host app at startup.
    return listOf(TimberLogWriter())
}
