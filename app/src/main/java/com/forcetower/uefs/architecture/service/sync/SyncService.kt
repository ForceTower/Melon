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

package com.forcetower.uefs.architecture.service.sync

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.storage.repository.MicroSyncRepository
import com.forcetower.uefs.core.storage.repository.SagresSyncRepository
import com.forcetower.uefs.service.NotificationCreator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SyncService : LifecycleService() {
    @Inject
    lateinit var generalRepository: SagresSyncRepository
    @Inject
    lateinit var microRepository: MicroSyncRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var notificationManager: NotificationManagerCompat
    private var shouldRequestSyncUpdate = false
    private var isForegroundService = false

    private val accessObserver = Observer<Access?> {
        if (it == null || !it.valid) {
            onAccessInvalidated()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("Created service ;)")
        microRepository.access.observe(this, accessObserver)
        notificationManager = NotificationManagerCompat.from(this)
        startComponent()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            STOP_SERVICE_ACTION -> stopComponent()
            else -> startComponent()
        }

        return Service.START_STICKY
    }

    private fun startComponent() {
        Timber.d("Start component")
        if (shouldRequestSyncUpdate) {
            Timber.d("Already requesting...")
            return
        }
        Timber.d("Start requesteer")
        shouldRequestSyncUpdate = true
        createNotification()
        updateDataForService()
    }

    private fun stopComponent() {
        if (!shouldRequestSyncUpdate) return
        shouldRequestSyncUpdate = false
    }

    private fun disable() {
        if (isForegroundService) {
            stopForeground(false)
            isForegroundService = false
            shouldRequestSyncUpdate = false
            removeNotification()
        }
    }

    private fun removeNotification() {
        stopForeground(true)
    }

    private fun createNotification() {
        val intent = Intent(this, SyncService::class.java).apply {
            action = STOP_SERVICE_ACTION
        }
        val pending = PendingIntent.getService(this, 0, intent, 0)
        val notification = NotificationCreator.createCookieSyncServiceNotification(this, pending)
        notificationManager.notify(SYNC_NOTIFICATION, notification)

        if (!isForegroundService) {
            ContextCompat.startForegroundService(
                applicationContext,
                Intent(applicationContext, this@SyncService.javaClass)
            )
            startForeground(SYNC_NOTIFICATION, notification)
            isForegroundService = true
        }
    }

    private fun updateDataForService(): Boolean = handler.postDelayed(
        {
            Timber.d("Will update data")
            updateData()
            if (shouldRequestSyncUpdate) {
                updateDataForService()
            } else {
                Timber.d("Request stopped...")
            }
        },
        UPDATE_DATA_INTERVAL
    )

    private fun updateData() {
        serviceScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    createNotification()
                    Timber.d("Running Calling the cops")
                    microRepository.callTheCops()
                } catch (error: Throwable) {
                    Timber.d("Failed with exception... It's probably gone")
                    disable()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel ongoing coroutines
        serviceJob.cancel()
    }

    // Access no longer exists... Service is no longer needed
    private fun onAccessInvalidated() {
        stopSelf()
    }

    companion object {
        const val STOP_SERVICE_ACTION = "com.forcetower.uefs.sync.STOP_FOREGROUND_SERVICE"
        const val UPDATE_DATA_INTERVAL = 450_000L // 15 minutes
        const val SYNC_NOTIFICATION: Int = 0xb751
    }
}
