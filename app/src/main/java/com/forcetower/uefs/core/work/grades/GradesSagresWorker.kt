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

package com.forcetower.uefs.core.work.grades

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.forcetower.uefs.core.storage.repository.SagresGradesRepository
import com.forcetower.uefs.core.work.enqueueUnique
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class GradesSagresWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: SagresGradesRepository
) : Worker(context, params) {
    override fun doWork(): Result {
        val semesterId = inputData.getLong(SEMESTER_ID, 0)
        return try {
            val result = repository.getGrades(semesterId)
            when {
                result >= 0 -> Result.success()
                result >= -2 -> Result.failure()
                else -> Result.retry()
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "grades_download_worker"
        private const val NAME = "worker_grades_downloader_"
        private const val SEMESTER_ID = "worker_semester_id"

        fun createWorker(context: Context, semesterId: Long) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(SEMESTER_ID to semesterId)

            val request = OneTimeWorkRequestBuilder<GradesSagresWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .setInputData(data)
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            request.enqueueUnique(context, NAME + semesterId, true)
        }
    }
}
