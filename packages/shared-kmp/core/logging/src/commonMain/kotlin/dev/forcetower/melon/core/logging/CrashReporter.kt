package dev.forcetower.melon.core.logging

// Host-provided bridge to the platform crash reporter (FirebaseCrashlytics on
// iOS/Android). The KMP framework stays free of any Firebase symbols — that
// avoids the CrashKiOS flat-namespace dyld failure where Umbrella.framework
// expected FIRCLS/FIRCrashlytics symbols to appear from the host app.
interface CrashReporter {
    fun log(message: String)
    fun recordNonFatal(message: String, throwable: Throwable?)
}

object NoopCrashReporter : CrashReporter {
    override fun log(message: String) = Unit
    override fun recordNonFatal(message: String, throwable: Throwable?) = Unit
}
