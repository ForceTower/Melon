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
import androidx.annotation.IntRange
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.forcetower.uefs.core.storage.repository.SagresSyncRepository
import com.forcetower.uefs.core.work.enqueueUnique
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncLinkedWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: SagresSyncRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            repository.performSync("Linked")
        } catch (t: Throwable) {
            Timber.d("Worker ignored the error so it may continue")
        }
        val period = inputData.getInt(PERIOD, 60)
        val count = inputData.getInt(COUNT, 0)
        val other = if (count == 2) 1 else 2
        createWorker(applicationContext, period, true, other)
        return Result.success()
    }

    companion object {
        private const val PERIOD = "linked_work_period"
        private const val COUNT = "linked_work_count"

        private const val TAG = "linked_sagres_sync_worker"
        private const val NAME = "worker_sagres_linked"

        fun createWorker(context: Context, @IntRange(from = 1, to = 9000) period: Int, replace: Boolean = true, count: Int = 0) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(PERIOD to period, COUNT to count)

            val request = OneTimeWorkRequestBuilder<SyncLinkedWorker>()
                .setInputData(data)
                .addTag(TAG)
                .setInitialDelay(period.toLong(), TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            request.enqueueUnique(context, "${NAME}_$count", replace)
            if (replace) {
                Timber.d("Scheduled linked worker on a $period period")
            }
        }

        fun stopWorker(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG).result.get()
        }
    }
}
