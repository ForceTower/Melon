package dev.forcetower.unes

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import dev.forcetower.melon.core.common.ForegroundSignal
import dev.forcetower.unes.firebase.FeatureFlags
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class MelonApp : Application() {

    @Inject
    internal lateinit var featureFlags: FeatureFlags

    @Inject
    internal lateinit var foregroundSignal: ForegroundSignal

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
        featureFlags.start()
        // Process-wide (not per-Activity) so time-derived KMP flows recompute
        // "today" the instant the app resumes, mirroring iOS `.sceneActivated`.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                foregroundSignal.pulse()
            }
        })
    }
}
