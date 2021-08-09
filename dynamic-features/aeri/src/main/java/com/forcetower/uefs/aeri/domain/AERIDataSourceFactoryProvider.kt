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

package com.forcetower.uefs.aeri.domain

import android.content.Context
import androidx.annotation.Keep
import com.forcetower.core.interfaces.DynamicDataSourceFactory
import com.forcetower.core.interfaces.DynamicDataSourceFactoryProvider
import com.forcetower.uefs.aeri.core.injection.DaggerAERIComponent
import com.forcetower.uefs.core.injection.dependencies.AERIModuleDependencies
import dagger.hilt.android.EntryPointAccessors

@Keep
class AERIDataSourceFactoryProvider : DynamicDataSourceFactoryProvider {
    override fun getFactory(context: Context): DynamicDataSourceFactory {
        return DaggerAERIComponent.builder()
            .context(context)
            .dependencies(
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    AERIModuleDependencies::class.java
                )
            ).build()
            .factory()
    }
}
