package dev.forcetower.melon.core.logging

import co.touchlab.kermit.LogWriter

// Holder for LogWriters contributed from modules that depend on core/logging.
// NetworkGraph can't contribute writers directly as a type core/logging sees
// (the dep only runs network → logging, not the reverse), so it instead hands
// back a RemoteLogWriters whose element type is the Kermit-provided LogWriter
// interface that both modules already have on the classpath.
class RemoteLogWriters(val writers: List<LogWriter>) {
    companion object {
        val Empty = RemoteLogWriters(emptyList())
    }
}
