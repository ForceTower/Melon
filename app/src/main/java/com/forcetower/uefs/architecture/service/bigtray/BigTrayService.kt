/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.uefs.architecture.service.bigtray

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.forcetower.uefs.core.model.bigtray.BigTrayData
import com.forcetower.uefs.feature.bigtray.BigTrayRepository
import com.forcetower.uefs.service.NotificationCreator
import dagger.android.AndroidInjection
import timber.log.Timber
import javax.inject.Inject

class BigTrayService : LifecycleService() {
    companion object {
        private const val NOTIFICATION_BIG_TRAY = 187745
        const val START_SERVICE_ACTION = "com.forcetower.uefs.bigtray.START_FOREGROUND_SERVICE"
        const val STOP_SERVICE_ACTION = "com.forcetower.uefs.bigtray.STOP_FOREGROUND_SERVICE"

        @JvmStatic
        fun startService(context: Context) {
            val intent = Intent(context, BigTrayService::class.java)
            context.startService(intent)
        }
    }

    @Inject
    lateinit var repository: BigTrayRepository
    private var running = false
    private var trayData: BigTrayData? = null

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when {
            intent?.action == START_SERVICE_ACTION -> startComponent()
            intent?.action == STOP_SERVICE_ACTION -> stopComponent()
            else -> startComponent()
        }

        return Service.START_STICKY
    }

    private fun stopComponent() {
        Timber.d("Stop service action")
        running = false
        stopForeground(true)
        stopSelf()
    }

    private fun startComponent() {
        if (!running) {
            running = true
            Timber.d("Start action!")
            startForeground(NOTIFICATION_BIG_TRAY, createNotification())
            repository.beginWith(7000).observe(this, Observer {
                if (trayData != it) {
                    trayData = it
                    startForeground(NOTIFICATION_BIG_TRAY, createNotification(it))
                }
            })
        } else {
            Timber.d("Ignored new run attempt while it's already running")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.requesting = false
        running = false
    }

    private fun createNotification(data: BigTrayData? = null): Notification {
        val intent = Intent(this, BigTrayService::class.java).apply {
            action = STOP_SERVICE_ACTION
        }
        val pending = PendingIntent.getService(this, 0, intent, 0)
        return NotificationCreator.showBigTrayNotification(this, data, pending)
    }
}
