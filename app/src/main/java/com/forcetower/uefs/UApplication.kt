package com.forcetower.uefs

import android.app.Application
import timber.log.Timber

class UApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}