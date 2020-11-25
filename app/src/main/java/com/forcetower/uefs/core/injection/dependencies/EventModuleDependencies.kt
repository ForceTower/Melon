package com.forcetower.uefs.core.injection.dependencies

import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient

@EntryPoint
@InstallIn(SingletonComponent::class)
interface EventModuleDependencies {
    fun coreDatabase(): UDatabase
    fun service(): UService
    fun client(): OkHttpClient
}
