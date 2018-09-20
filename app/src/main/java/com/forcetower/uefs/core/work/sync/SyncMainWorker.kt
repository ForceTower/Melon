package com.forcetower.uefs.core.work.sync

import android.content.Context
import android.preference.PreferenceManager
import androidx.annotation.IntRange
import androidx.annotation.WorkerThread
import androidx.work.*
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.constants.PreferenceConstants
import com.forcetower.uefs.core.storage.repository.SagresSyncRepository
import com.forcetower.uefs.core.work.enqueueUnique
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SyncMainWorker(
    context : Context, params : WorkerParameters
): Worker(context, params) {
    @Inject
    lateinit var repository: SagresSyncRepository

    @WorkerThread
    override fun doWork(): Result {
        (applicationContext as UApplication).component.inject(this)
        Timber.d("Main Worker started")
        repository.performSync()
        Timber.d("Main Worker completed")
        return Result.SUCCESS
    }

    companion object {
        private const val TAG = "main_sagres_sync_worker"
        private const val NAME = "worker_sagres_sync"

        //Function that creates a Sagres Sync Worker
        fun createWorker(ctx: Context, @IntRange(from = 15, to = 9000) period: Int) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
            //We need to observe the frequency to know if we need to replace the current worker with a new one
            val current = preferences.getInt(PreferenceConstants.SYNC_FREQUENCY, 60)
            val replace = current != period

            //The Sync Worker requires internet connection
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            //This worker is periodic
            val request = PeriodicWorkRequestBuilder<SyncMainWorker>(period.toLong(), TimeUnit.MINUTES)
                    .addTag(TAG)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                    .build()

            request.enqueueUnique(NAME, replace)
            Timber.d("Main Sync Work Scheduled")
        }

        fun stopWorker() {
            WorkManager.getInstance().cancelAllWorkByTag(TAG)
        }
    }
}