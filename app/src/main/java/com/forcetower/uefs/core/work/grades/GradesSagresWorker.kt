package com.forcetower.uefs.core.work.grades

import androidx.work.*
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.storage.repository.SagresGradesRepository
import com.forcetower.uefs.core.work.enqueueUnique
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GradesSagresWorker: Worker() {
    @Inject
    lateinit var repository: SagresGradesRepository
    override fun doWork(): Result {
        (applicationContext as UApplication).component.inject(this)
        val semesterId = inputData.getLong(SEMESTER_ID, 0)
        val result = repository.getGrades(semesterId)
        return when {
            result >=  0 -> Result.SUCCESS
            result >= -2 -> Result.FAILURE
            else -> Result.RETRY
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