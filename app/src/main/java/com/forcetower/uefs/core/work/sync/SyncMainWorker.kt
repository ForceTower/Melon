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

package com.forcetower.uefs.core.work.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.annotation.IntRange
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.*
import com.forcetower.uefs.core.constants.PreferenceConstants
import com.forcetower.uefs.core.storage.repository.SagresSyncRepository
import com.forcetower.uefs.core.storage.repository.SnowpiercerSyncRepository
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.core.work.enqueueUnique
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SyncMainWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {
    @Inject lateinit var repository: SagresSyncRepository
    @Inject lateinit var snowpiercer: SnowpiercerSyncRepository
    @Inject lateinit var remoteConfig: FirebaseRemoteConfig
    @Inject lateinit var preferences: SharedPreferences

    override suspend fun doWork(): Result {
        try {
            Timber.d("Main Worker started")
            if (preferences.isStudentFromUEFS() && remoteConfig.getBoolean("feature_flag_use_snowpiercer")) {
                snowpiercer.performSync("Snowpiercer")
            } else {
                repository.performSync("Principal")
            }
            Timber.d("Main Worker completed")
        } catch (t: Throwable) {
            Timber.d("Worker ignored the error so it may continue")
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "main_sagres_sync_worker"
        private const val NAME = "worker_sagres_sync"

        // Function that creates a Sagres Sync Worker
        fun createWorker(ctx: Context, @IntRange(from = 15, to = 9000) period: Int, forcedReplace: Boolean = false) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
            // We need to observe the frequency to know if we need to replace the current worker with a new one
            val current = preferences.getInt(PreferenceConstants.SYNC_FREQUENCY, 60)
            val replace = current != period || forcedReplace

            // The Sync Worker requires internet connection
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // This worker is periodic
            val request = PeriodicWorkRequestBuilder<SyncMainWorker>(period.toLong(), TimeUnit.MINUTES)
                .addTag(TAG)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            request.enqueueUnique(ctx, NAME, replace)
            if (replace) preferences.edit().putInt(PreferenceConstants.SYNC_FREQUENCY, period).apply()
            Timber.d("Main Sync Work Scheduled")
        }

        fun stopWorker(ctx: Context) {
            WorkManager.getInstance(ctx).cancelAllWorkByTag(TAG)
        }
    }
}