package com.forcetower.uefs.core.work.sync

import android.content.Context
import androidx.annotation.IntRange
import androidx.work.*
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.storage.repository.SagresSyncRepository
import com.forcetower.uefs.core.work.enqueueUnique
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SyncLinkedWorker(
    context : Context, params : WorkerParameters
): Worker(context, params) {
    @Inject
    lateinit var repository: SagresSyncRepository

    override fun doWork(): Result {
        (applicationContext as UApplication).component.inject(this)
        repository.performSync()

        val period = inputData.getInt(PERIOD, 60)
        createWorker(period, false)
        return Result.SUCCESS
    }

    companion object {
        private const val PERIOD = "linked_work_period"

        private const val TAG = "linked_sagres_sync_worker"
        private const val NAME = "worker_sagres_linked"

        fun createWorker(@IntRange(from = 1, to = 9000) period: Int, replace: Boolean = true) {
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val data = workDataOf(PERIOD to period)

            val request = OneTimeWorkRequestBuilder<SyncLinkedWorker>()
                    .setInputData(data)
                    .addTag(TAG)
                    .setInitialDelay(period.toLong(), TimeUnit.MINUTES)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

            request.enqueueUnique(NAME, replace)
        }

        fun stopWorker() {
            WorkManager.getInstance().cancelAllWorkByTag(TAG)
        }
    }
}