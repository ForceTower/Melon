package com.forcetower.uefs.core.storage.repository

import androidx.annotation.MainThread
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.storage.database.UDatabase
import javax.inject.Inject

class AdventureRepository @Inject constructor(
    private val database: UDatabase,
    private val executors: AppExecutors
) {

    @MainThread
    fun checkAchievements() {
        executors.diskIO().execute { internalCheckAchievements() }
    }

    private fun internalCheckAchievements() {

    }
}
