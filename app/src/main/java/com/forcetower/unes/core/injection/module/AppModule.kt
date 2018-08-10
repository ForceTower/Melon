package com.forcetower.unes.core.injection.module

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.forcetower.unes.UApplication
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object AppModule {

    @Provides
    @Singleton
    @JvmStatic
    fun provideContext(application: UApplication): Context = application.applicationContext

    @Provides
    @Singleton
    @JvmStatic
    fun provideSharedPreferences(context: Context): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)

}