package com.forcetower.uefs.core.work.sync

import androidx.annotation.WorkerThread
import androidx.work.Worker
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.storage.repository.LoginSagresRepository
import javax.inject.Inject

class SyncMainWorker: Worker() {
    @Inject
    lateinit var repository: LoginSagresRepository

    @WorkerThread
    override fun doWork(): Result {
        (applicationContext as UApplication).component.inject(this)
        return Result.SUCCESS
    }
}