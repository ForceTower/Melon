package com.forcetower.uefs.core.work

import androidx.work.*

fun OneTimeWorkRequest.enqueueUnique(name: String, replace: Boolean = true) {
    WorkManager.getInstance().beginUniqueWork(
            name,
            if (replace)
                ExistingWorkPolicy.REPLACE
            else
                ExistingWorkPolicy.KEEP,
            this
    ).enqueue()
}

fun PeriodicWorkRequest.enqueueUnique(name: String, replace: Boolean = true) {
    WorkManager.getInstance().enqueueUniquePeriodicWork(
            name,
            if (replace)
                ExistingPeriodicWorkPolicy.REPLACE
            else
                ExistingPeriodicWorkPolicy.KEEP,
            this
    )
}