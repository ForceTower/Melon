package dev.forcetower.melon.core.logging

import co.touchlab.kermit.LogWriter

// Writers contributed by the host platform:
//   - iOS: OSLogWriter (console / Console.app) + Crashlytics writer (non-fatals).
//   - JVM: CommonWriter (println) — used for tests and scripting entry points.
// Remote/OTLP shipping lives in commonMain so the wire format stays identical
// across platforms.
internal expect fun platformDefaultLogWriters(config: LoggingConfig): List<LogWriter>
