package dev.forcetower.melon.umbrella

import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.NoOpAnalytics
import dev.forcetower.melon.core.common.ApplicationContext
import dev.forcetower.melon.core.logging.CrashReporter
import dev.forcetower.melon.core.logging.LoggingConfig

data class UmbrellaConfig(
    val baseUrl: String,
    val appContext: ApplicationContext,
    val logging: LoggingConfig = LoggingConfig(),
    // Host-provided crash reporter. Null means no non-fatals are filed (the
    // CrashReporterLogWriter is skipped). iOS passes a wrapper around
    // Crashlytics.crashlytics(); Android will pass a FirebaseCrashlytics
    // wrapper when that app exists.
    val crashReporter: CrashReporter? = null,
    // Host-provided product-analytics sink. Defaults to no-op so tests and any
    // headless consumer stay silent; Android passes a PostHog wrapper.
    val analytics: Analytics = NoOpAnalytics,
)
