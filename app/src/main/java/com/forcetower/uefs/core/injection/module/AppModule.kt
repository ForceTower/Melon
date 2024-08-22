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
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.forcetower.uefs.core.storage.apidatabase.APIDatabase
import com.forcetower.uefs.core.storage.database.M50TO51
import com.forcetower.uefs.core.storage.database.M51TO52
import com.forcetower.uefs.core.storage.database.M52TO53
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
            .addMigrations(M50TO51, M51TO52, M52TO53)
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
    @Named("internalConfig")
    fun provideFlagsConfig(context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("internal_config")
        }
    }

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
    fun provideWebViewUserAgent(): String {
        val agents = listOf(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Safari/605.1.15",
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5.2 (a) Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 14; SM-S928B Build/UP1A.231005.007; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/127.0.6533.103 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 10; MI 9 Build/QKQ1.190825.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/123.0.6312.118 Mobile Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36"
        )
        return agents.random()
    }

    @Provides
    @Reusable
    @Named("unesUserAgent")
    fun provideUnesUserAgent(context: Context): String {
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        val parts = version.split(".")
        return "UNES/${parts[0]}.${parts[1]}.${parts[2]}.${parts[3]} (Android ${Build.VERSION.RELEASE}; Build/${Build.MODEL})"
    }
}
