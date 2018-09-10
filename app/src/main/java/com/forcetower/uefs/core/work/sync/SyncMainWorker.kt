package com.forcetower.uefs.core.work.sync

import androidx.annotation.WorkerThread
import androidx.work.Worker
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.storage.repository.SagresSyncRepository
import javax.inject.Inject

class SyncMainWorker: Worker() {
    @Inject
    lateinit var repository: SagresSyncRepository

    @WorkerThread
    override fun doWork(): Result {
        (applicationContext as UApplication).component.inject(this)
        repository.performSync()
        return Result.SUCCESS
    }
}