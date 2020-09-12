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

package dev.forcetower.event.core.injection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.forcetower.core.base.BaseViewModelFactory
import com.forcetower.core.injection.annotation.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck
import dagger.multibindings.IntoMap
import dev.forcetower.event.feature.create.CreationViewModel
import dev.forcetower.event.feature.details.EventDetailsViewModel
import dev.forcetower.event.feature.listing.EventViewModel

@Module
@DisableInstallInCheck
abstract class FeatureViewModels {
    @Binds
    @IntoMap
    @ViewModelKey(EventViewModel::class)
    abstract fun events(viewModel: EventViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EventDetailsViewModel::class)
    abstract fun details(viewModel: EventDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CreationViewModel::class)
    abstract fun create(viewModel: CreationViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: BaseViewModelFactory): ViewModelProvider.Factory
}
