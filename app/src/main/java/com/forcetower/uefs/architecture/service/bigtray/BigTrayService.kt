package com.forcetower.uefs.architecture.service.bigtray

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.forcetower.uefs.core.model.bigtray.BigTrayData
import com.forcetower.uefs.feature.bigtray.BigTrayRepository
import com.forcetower.uefs.service.NotificationCreator
import dagger.android.AndroidInjection
import timber.log.Timber
import javax.inject.Inject

class BigTrayService: LifecycleService(), LifecycleOwner {
    companion object {
        private const val NOTIFICATION_BIG_TRAY = 187745
        private const val STOP_SERVICE_ACTION = "com.forcetower.uefs.bigtray.STOP_FOREGROUND_SERVICE"

        @JvmStatic
        fun startService(context: Context) {
            val intent = Intent(context, BigTrayService::class.java)
            context.startService(intent)
        }
    }

    @Inject
    lateinit var repository: BigTrayRepository
    private var running = false

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when {
            intent == null -> {
                if (!running) {
                    running = true
                    Timber.d("No intent received!! Start action!")
                    startForeground(NOTIFICATION_BIG_TRAY, createNotification())
                    repository.data.observe(this, Observer {
                        startForeground(NOTIFICATION_BIG_TRAY, createNotification(it))
                    })
                } else {
                    Timber.d("Ignored new run attempt while it's already running")
                }
            }
            intent.action == STOP_SERVICE_ACTION -> {
                Timber.d("Stop service action")
                running = false
                stopForeground(true)
                stopSelf()
            }
        }

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.requesting = false
    }

    private fun createNotification(data: BigTrayData? = null): Notification {
        val intent = Intent(this, BigTrayService::class.java)
        val pending = PendingIntent.getService(this, 0, intent, 0)
        return NotificationCreator.showBigTrayNotification(this, data, pending)
    }

}
