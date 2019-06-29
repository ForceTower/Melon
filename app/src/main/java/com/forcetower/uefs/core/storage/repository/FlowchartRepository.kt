package com.forcetower.uefs.core.storage.repository

import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import javax.inject.Inject

class FlowchartRepository @Inject constructor(
    private val database: UDatabase,
    private val service: UService,
    private val executors: AppExecutors
)