package dev.forcetower.melon.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import timber.log.Timber

// Bridges Kermit (which the rest of the codebase logs through) into Timber.
// Timber owns the Android-side tree configuration (DebugTree for debug builds,
// a Crashlytics tree for release, etc.) — this writer just forwards the
// already-routed Kermit event to whichever trees are planted.
internal class TimberLogWriter : LogWriter() {
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val tree = Timber.tag(tag)
        when (severity) {
            Severity.Verbose -> tree.v(throwable, message)
            Severity.Debug -> tree.d(throwable, message)
            Severity.Info -> tree.i(throwable, message)
            Severity.Warn -> tree.w(throwable, message)
            Severity.Error -> tree.e(throwable, message)
            Severity.Assert -> tree.wtf(throwable, message)
        }
    }
}
