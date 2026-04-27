package dev.forcetower.unes.firebase

import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.forcetower.melon.core.logging.CrashReporter

// Host-side `CrashReporter` for Android, mirroring `FirebaseCrashReporter` in
// `apps/ios/UNES/App/Logging`. The KMP framework deliberately doesn't link
// FirebaseCrashlytics — it just hands breadcrumbs / non-fatals to whatever
// reporter the host wires in, so the SDK only ships in the Android APK.
internal class FirebaseCrashReporter(
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance(),
) : CrashReporter {
    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun recordNonFatal(message: String, throwable: Throwable?) {
        if (throwable != null) {
            crashlytics.log(message)
            crashlytics.recordException(throwable)
        } else {
            crashlytics.recordException(RuntimeException(message))
        }
    }
}
