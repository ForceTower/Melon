package com.forcetower.unes.core.storage.repository

import com.forcetower.unes.AppExecutors
import com.forcetower.unes.core.storage.database.UDatabase
import javax.inject.Inject

class UserRepository @Inject constructor(
        private val database: UDatabase,
        private val executor: AppExecutors
) {

    fun getAccess() = database.accessDao().getAccess()
}
