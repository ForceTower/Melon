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
import com.forcetower.uefs.feature.themeswitcher.ThemeSwitcherResourceProvider
import com.forcetower.uefs.GooglePlayGamesInstance
import com.forcetower.uefs.core.storage.apidatabase.APIDatabase
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.database.M1TO2
import com.forcetower.uefs.core.storage.database.M2TO3
import com.forcetower.uefs.core.storage.database.M3TO4
import com.forcetower.uefs.core.storage.database.M5TO6
import com.forcetower.uefs.core.storage.database.M6TO7
import com.forcetower.uefs.core.storage.database.M7TO8
import com.forcetower.uefs.core.storage.database.M8TO9
import com.forcetower.uefs.core.storage.database.M9TO10
import com.forcetower.uefs.core.storage.database.M10TO11
import com.forcetower.uefs.core.storage.database.M11TO12
import com.forcetower.uefs.core.storage.database.M12TO13
import com.forcetower.uefs.core.storage.database.M13TO14
import com.forcetower.uefs.core.storage.database.M14TO15
import com.forcetower.uefs.core.storage.database.M15TO16
import com.forcetower.uefs.core.storage.database.M16TO17
import com.forcetower.uefs.core.storage.database.M17TO18
import com.forcetower.uefs.core.storage.database.M18TO19
import com.forcetower.uefs.core.storage.database.M19TO20
import com.forcetower.uefs.core.storage.database.M20TO21
import com.forcetower.uefs.core.storage.database.M21TO22
import com.forcetower.uefs.core.storage.database.M22TO23
import com.forcetower.uefs.core.storage.database.M23TO24
import com.forcetower.uefs.core.storage.database.M24TO25
import com.forcetower.uefs.core.storage.database.M25TO26
import com.forcetower.uefs.core.storage.database.M26TO27
import com.forcetower.uefs.core.storage.database.M27TO28
import com.forcetower.uefs.core.storage.database.M28TO29
import com.forcetower.uefs.core.storage.database.M29TO30
import com.forcetower.uefs.core.storage.database.M30TO31
import com.forcetower.uefs.core.storage.database.M31TO32
import com.forcetower.uefs.core.storage.database.M32TO33
import com.forcetower.uefs.core.storage.database.M33TO34
import com.forcetower.uefs.core.storage.database.M34TO35
import com.forcetower.uefs.core.storage.database.M35TO36
import com.forcetower.uefs.core.storage.database.M36TO37
import com.forcetower.uefs.core.storage.database.M37TO38
import com.forcetower.uefs.core.storage.database.M38TO39
import com.forcetower.uefs.core.storage.database.M39TO40
import com.forcetower.uefs.core.storage.database.M40TO41
import com.forcetower.uefs.core.storage.database.M41TO42
import com.forcetower.uefs.core.storage.database.M42TO43
import com.forcetower.uefs.core.storage.database.M43TO44
import com.forcetower.uefs.core.storage.database.M44TO45
import com.forcetower.uefs.core.storage.database.M45TO46
import com.forcetower.uefs.core.storage.database.M45TO47
import com.forcetower.uefs.core.storage.database.M46TO47
import com.forcetower.uefs.core.storage.database.M47TO48
import com.forcetower.uefs.core.storage.eventdatabase.EventDatabase
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Reusable
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
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
                .addMigrations()
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
    @Reusable
    @Named("flagSnowpiercerEnabled")
    fun provideFlagSnowpiercer(preferences: SharedPreferences, remoteConfig: FirebaseRemoteConfig) =
        preferences.isStudentFromUEFS() && remoteConfig.getBoolean("feature_flag_use_snowpiercer")

    @Provides
    @Reusable
    @Named("webViewUA")
    fun provideWebViewUserAgent(context: Context): String = WebSettings.getDefaultUserAgent(context)
}