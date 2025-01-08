package com.forcetower.uefs.impl

import android.util.Log
import com.datadog.android.log.Logger
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatadogTree @Inject constructor(
    private val logger: Logger
) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority <= Log.DEBUG) return

        val msg = if (tag != null) "$tag: $message" else message
        logger.log(priority, msg, t)
    }
}