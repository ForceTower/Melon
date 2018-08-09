package com.forcetower.uefs_2

import android.app.Application
import timber.log.Timber

class UApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}