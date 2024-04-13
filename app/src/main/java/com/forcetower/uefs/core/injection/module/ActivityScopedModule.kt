package com.forcetower.uefs.core.injection.module

import android.app.Activity
import com.forcetower.uefs.GooglePlayGamesInstance
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
object ActivityScopedModule {
    @Provides
    fun providePlayGames(activity: Activity): GooglePlayGamesInstance =
        GooglePlayGamesInstance(activity)
}
