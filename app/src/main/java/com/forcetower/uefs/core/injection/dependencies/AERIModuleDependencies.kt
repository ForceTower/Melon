package com.forcetower.uefs.core.injection.dependencies

import com.forcetower.uefs.AppExecutors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AERIModuleDependencies {
    fun executors(): AppExecutors
}
