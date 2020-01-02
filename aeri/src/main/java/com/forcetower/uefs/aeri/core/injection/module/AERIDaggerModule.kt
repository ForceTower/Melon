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

package com.forcetower.uefs.aeri.core.injection.module

import android.content.Context
import androidx.room.Room
import com.forcetower.core.injection.annotation.FeatureScope
import com.forcetower.uefs.aeri.core.storage.database.AERIDatabase
import dagger.Module
import dagger.Provides

@Module
object AERIDaggerModule {

    @Provides
    @FeatureScope
    fun provideDatabase(context: Context): AERIDatabase {
        return Room.databaseBuilder(context, AERIDatabase::class.java, "aeri_data.db")
            .enableMultiInstanceInvalidation()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }
}