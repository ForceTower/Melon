package com.forcetower.unes.core.injection.module

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.room.Room
import com.forcetower.unes.UApplication
import com.forcetower.unes.core.storage.database.UDatabase
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


    @Provides
    @Singleton
    @JvmStatic
    fun provideDatabase(context: Context): UDatabase =
            Room.databaseBuilder(context.applicationContext, UDatabase::class.java, "unesco.db")
                    .fallbackToDestructiveMigration()
                    .build()

}