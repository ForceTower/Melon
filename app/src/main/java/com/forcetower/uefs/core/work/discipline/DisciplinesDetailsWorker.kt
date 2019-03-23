package com.forcetower.uefs.core.work.discipline

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.storage.repository.DisciplineDetailsRepository
import com.forcetower.uefs.core.work.enqueueUnique
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DisciplinesDetailsWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    @Inject
    lateinit var repository: DisciplineDetailsRepository

    @WorkerThread
    override fun doWork(): Result {
        (applicationContext as UApplication).component.inject(this)
        return try {
            inputData.getBoolean("partial", true)
            repository.experimentalDisciplines(partialLoad = true, notify = false)
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "discipline_details_worker"
        private const val NAME = "discipline_details"

        fun createWorker() {
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val request = OneTimeWorkRequestBuilder<DisciplinesDetailsWorker>()
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .addTag(TAG)
                    .build()

            request.enqueueUnique(NAME, true)
        }
    }
}
