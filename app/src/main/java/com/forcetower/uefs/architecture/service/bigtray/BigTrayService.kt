/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            START_SERVICE_ACTION -> startComponent()
            STOP_SERVICE_ACTION -> stopComponent()
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
            repository.beginWith(7000).observe(
                this,
                Observer {
                    if (trayData != it) {
                        trayData = it
                        startForeground(NOTIFICATION_BIG_TRAY, createNotification(it))
                    }
                }
            )
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
