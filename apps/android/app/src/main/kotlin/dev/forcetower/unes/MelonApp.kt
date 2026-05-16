package dev.forcetower.unes

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MelonApp : Application() {

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
    }
}
