package dev.forcetower.melon.core.logging

import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.LogWriter

internal actual fun platformDefaultLogWriters(config: LoggingConfig): List<LogWriter> {
    // CommonWriter writes everything it receives; the Logger itself enforces
    // minLocalSeverity (see LoggingGraph).
    return listOf(CommonWriter())
}
