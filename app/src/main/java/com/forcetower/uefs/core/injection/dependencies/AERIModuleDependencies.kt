package com.forcetower.uefs.core.injection.dependencies

import com.forcetower.uefs.AppExecutors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent

@EntryPoint
@InstallIn(ApplicationComponent::class)
interface AERIModuleDependencies {
    fun executors(): AppExecutors
}