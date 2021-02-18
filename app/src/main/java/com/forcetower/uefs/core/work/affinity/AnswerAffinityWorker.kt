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

package com.forcetower.uefs.core.work.affinity

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.forcetower.uefs.core.storage.repository.cloud.AffinityQuestionRepository
import com.forcetower.uefs.core.work.enqueue
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class AnswerAffinityWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AffinityQuestionRepository
) : Worker(context, params) {
    override fun doWork(): Result {
        Timber.d("Answering affinity")

        val questionId = inputData.getLong("question_id", 0)
        val studentId = inputData.getLong("student_id", 0)
        if (questionId == 0L || studentId == 0L) {
            return Result.failure()
        }
        val result = repository.answerAffinityQuestion(questionId, studentId)
        return if (result) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "answer_affinity_worker"
        private const val QUESTION_ID = "question_id"
        private const val STUDENT_ID = "student_id"

        fun createWorker(context: Context, questionId: Long, studentId: Long) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(
                QUESTION_ID to questionId,
                STUDENT_ID to studentId
            )

            val request = OneTimeWorkRequestBuilder<AnswerAffinityWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .setInputData(data)
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            request.enqueue(context)
            Timber.d("Enqueue answer affinity")
        }
    }
}
