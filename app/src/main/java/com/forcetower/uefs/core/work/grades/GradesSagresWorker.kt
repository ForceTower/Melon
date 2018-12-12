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

package com.forcetower.uefs.core.work.grades

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.Result
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.storage.repository.SagresGradesRepository
import com.forcetower.uefs.core.work.enqueueUnique
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GradesSagresWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    @Inject
    lateinit var repository: SagresGradesRepository
    override fun doWork(): Result {
        (applicationContext as UApplication).component.inject(this)
        val semesterId = inputData.getLong(SEMESTER_ID, 0)
        val result = repository.getGrades(semesterId)
        return when {
            result >= 0 -> Result.success()
            result >= -2 -> Result.failure()
            else -> Result.retry()
        }
    }

    companion object {
        private const val TAG = "grades_download_worker"
        private const val NAME = "worker_grades_downloader_"
        private const val SEMESTER_ID = "worker_semester_id"

        fun createWorker(semesterId: Long) {
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

            request.enqueueUnique(NAME + semesterId, true)
        }
    }
}