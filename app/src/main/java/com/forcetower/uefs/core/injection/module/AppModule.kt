/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.core.injection.module

import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebSettings
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.forcetower.uefs.GooglePlayGamesInstance
import com.forcetower.uefs.core.storage.apidatabase.APIDatabase
import com.forcetower.uefs.core.storage.database.M50TO51
import com.forcetower.uefs.core.storage.database.M51TO52
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.eventdatabase.EventDatabase
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.feature.themeswitcher.ThemeSwitcherResourceProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideSharedPreferences(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    @Provides
    @Singleton
    fun provideDatabase(context: Context): UDatabase =
        Room.databaseBuilder(context.applicationContext, UDatabase::class.java, "unespiercer.db")
            .addMigrations(M50TO51, M51TO52)
            .enableMultiInstanceInvalidation()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    @Singleton
    fun provideApiDatabase(context: Context): APIDatabase =
        Room.databaseBuilder(context.applicationContext, APIDatabase::class.java, "unesglass.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideEventDatabase(context: Context): EventDatabase =
        Room.databaseBuilder(context.applicationContext, EventDatabase::class.java, "unevents.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun providePlayGames(context: Context): GooglePlayGamesInstance =
        GooglePlayGamesInstance(context)

    @Provides
    @Singleton
    fun provideThemeSwitcherResourceProvider() = ThemeSwitcherResourceProvider()

    @Provides
    @Named("flagSnowpiercerEnabled")
    fun provideFlagSnowpiercer(preferences: SharedPreferences, remoteConfig: FirebaseRemoteConfig) =
        preferences.isStudentFromUEFS() && remoteConfig.getBoolean("feature_flag_use_snowpiercer")

    @Provides
    @Reusable
    @Named("webViewUA")
    fun provideWebViewUserAgent(context: Context): String {
        return try {
            WebSettings.getDefaultUserAgent(context)
        } catch (error: Throwable) {
            Timber.w("Failed to obtain device UserAgent")
            Timber.w("UserAgent error ${error.message}")
            "Mozilla/5.0 (Linux; Android 10; MI 9 Build/QKQ1.190825.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/84.0.4147.125 Mobile Safari/537.36"
        }
    }
}
