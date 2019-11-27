/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.forcetower.uefs.GooglePlayGamesInstance
import com.forcetower.uefs.UApplication
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
import com.forcetower.uefs.core.storage.eventdatabase.EventDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object AppModule {

    @Provides
    @Singleton
    @JvmStatic
    fun provideContext(application: UApplication): Context =
            application.applicationContext

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
                .addMigrations(
                    M1TO2, M2TO3, M3TO4, M5TO6, M6TO7, M7TO8, M8TO9, M9TO10, M10TO11, M11TO12,
                    M12TO13, M13TO14, M14TO15, M15TO16, M16TO17, M17TO18, M18TO19, M19TO20, M20TO21,
                    M21TO22, M22TO23, M23TO24, M24TO25, M25TO26, M26TO27, M27TO28, M28TO29, M29TO30,
                    M30TO31, M31TO32, M32TO33, M33TO34, M34TO35, M35TO36, M36TO37, M37TO38
                )
                .enableMultiInstanceInvalidation()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()

    @Provides
    @Singleton
    @JvmStatic
    fun provideApiDatabase(context: Context): APIDatabase =
            Room.databaseBuilder(context.applicationContext, APIDatabase::class.java, "unesglass.db")
                .fallbackToDestructiveMigration()
                .build()

    @Provides
    @Singleton
    @JvmStatic
    fun provideEventDatabase(context: Context): EventDatabase =
            Room.databaseBuilder(context.applicationContext, EventDatabase::class.java, "unevents.db")
                    .fallbackToDestructiveMigration()
                    .build()

    @Provides
    @Singleton
    @JvmStatic
    fun providePlayGames(context: Context): GooglePlayGamesInstance =
            GooglePlayGamesInstance(context)
}