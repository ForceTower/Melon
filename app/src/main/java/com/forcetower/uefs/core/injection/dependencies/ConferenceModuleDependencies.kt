package com.forcetower.uefs.core.injection.dependencies

import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import okhttp3.OkHttpClient

@EntryPoint
@InstallIn(ApplicationComponent::class)
interface ConferenceModuleDependencies {
    fun executors(): AppExecutors
    fun coreDatabase(): UDatabase
    fun service(): UService
    fun client(): OkHttpClient
}