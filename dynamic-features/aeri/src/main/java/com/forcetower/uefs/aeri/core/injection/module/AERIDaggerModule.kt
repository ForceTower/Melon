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
import com.forcetower.core.interfaces.DynamicDataSourceFactory
import com.forcetower.uefs.aeri.core.storage.database.AERIDatabase
import com.forcetower.uefs.aeri.core.storage.repository.AERIRepository
import com.forcetower.uefs.aeri.domain.AERIDataSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck

@Module
@DisableInstallInCheck
object AERIDaggerModule {
    @Provides
    fun provideDatabase(context: Context): AERIDatabase {
        return Room.databaseBuilder(context, AERIDatabase::class.java, "aeri_data.db")
            .enableMultiInstanceInvalidation()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideDataSource(repository: AERIRepository): DynamicDataSourceFactory {
        return AERIDataSourceFactory(repository)
    }
}
