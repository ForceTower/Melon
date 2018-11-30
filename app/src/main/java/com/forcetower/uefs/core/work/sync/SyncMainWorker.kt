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

package com.forcetower.uefs.core.work.sync

import android.content.Context
import android.preference.PreferenceManager
import androidx.annotation.IntRange
import androidx.annotation.WorkerThread
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.constants.PreferenceConstants
import com.forcetower.uefs.core.storage.repository.SagresSyncRepository
import com.forcetower.uefs.core.work.enqueueUnique
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SyncMainWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    @Inject
    lateinit var repository: SagresSyncRepository

    @WorkerThread
    override fun doWork(): Result {
        (applicationContext as UApplication).component.inject(this)
        Timber.d("Main Worker started")
        repository.performSync("Principal")
        Timber.d("Main Worker completed")
        return Result.SUCCESS
    }

    companion object {
        private const val TAG = "main_sagres_sync_worker"
        private const val NAME = "worker_sagres_sync"

        // Function that creates a Sagres Sync Worker
        fun createWorker(ctx: Context, @IntRange(from = 15, to = 9000) period: Int) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
            // We need to observe the frequency to know if we need to replace the current worker with a new one
            val current = preferences.getInt(PreferenceConstants.SYNC_FREQUENCY, 60)
            val replace = current != period

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

            request.enqueueUnique(NAME, replace)
            if (replace) preferences.edit().putInt(PreferenceConstants.SYNC_FREQUENCY, period).apply()
            Timber.d("Main Sync Work Scheduled")
        }

        fun stopWorker() {
            WorkManager.getInstance().cancelAllWorkByTag(TAG)
        }
    }
}