package com.forcetower.uefs.core.injection.dependencies

import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.repository.SagresDataRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface DashboardModuleDependencies {
    fun executors(): AppExecutors
    fun coreDatabase(): UDatabase
    fun dataRepository(): SagresDataRepository
}
