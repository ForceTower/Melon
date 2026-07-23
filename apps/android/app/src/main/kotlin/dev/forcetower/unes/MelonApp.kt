package dev.forcetower.unes

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import dagger.hilt.android.HiltAndroidApp
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.common.ForegroundSignal
import dev.forcetower.melon.core.network.MachineIdSource
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.session.domain.model.AuthState
import dev.forcetower.unes.di.ApplicationScope
import dev.forcetower.unes.firebase.FeatureFlags
import dev.forcetower.unes.firebase.PushRegistrar
import dev.forcetower.unes.reminders.EvaluationReminderScheduler
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
internal class MelonApp : Application() {
    @Inject
    lateinit var featureFlags: FeatureFlags

    @Inject
    lateinit var foregroundSignal: ForegroundSignal

    @Inject
    lateinit var sessionStore: SessionStore

    @Inject
    lateinit var analytics: Analytics

    @Inject
    lateinit var machineIdSource: MachineIdSource

    @Inject
    lateinit var evaluationReminders: EvaluationReminderScheduler

    @Inject
    lateinit var pushRegistrar: PushRegistrar

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Don't ship crashes from local builds. Mirrors iOS AppDelegate.swift,
        // which calls `setCrashlyticsCollectionEnabled(false)` under #if DEBUG.
        // Firebase itself auto-initializes via FirebaseInitProvider, so we
        // don't need to call FirebaseApp.initializeApp() here.
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

        PostHogAndroid.setup(
            this,
            PostHogAndroidConfig(
                apiKey = BuildConfig.POSTHOG_API_KEY,
                host = BuildConfig.POSTHOG_HOST,
            ).apply {
                debug = BuildConfig.DEBUG
                optOut = BuildConfig.DEBUG
                captureScreenViews = false
                sessionReplay = false
            },
        )
        // Analytics identity: stamp the device machine_id on every event (so
        // PostHog lines up with the OTel logs, which key on the same id), then
        // follow the session from one place — identify on login, reset on
        // logout. The initial `Unauthenticated` seed must NOT reset (that would
        // rotate the anonymous id every launch), so only reset after a real
        // login → logout transition; reset() also drops super-properties, so
        // re-stamp machine_id afterwards.
        applicationScope.launch {
            val machineId = machineIdSource.getMachineId()
            analytics.register(mapOf("machine_id" to machineId))
            var identified = false
            sessionStore.authState
                .map { (it as? AuthState.Authenticated)?.user?.id }
                .distinctUntilChanged()
                .collect { userId ->
                    when {
                        userId != null -> {
                            analytics.identify(userId)
                            identified = true
                        }
                        identified -> {
                            analytics.reset()
                            analytics.register(mapOf("machine_id" to machineId))
                            identified = false
                        }
                    }
                }
        }
        featureFlags.start()
        // App-lifetime like the analytics collector: also runs when a boot
        // broadcast spins the process up, so alarms re-anchor to fresh data.
        evaluationReminders.start()
        // Process-wide (not per-Activity) so time-derived KMP flows recompute
        // "today" the instant the app resumes, mirroring iOS `.sceneActivated`.
        // Every app open also re-sends the push registration, so the backend
        // always holds this device's current identifier.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                foregroundSignal.pulse()
                applicationScope.launch { pushRegistrar.reconcile() }
            }
        })
    }
}
