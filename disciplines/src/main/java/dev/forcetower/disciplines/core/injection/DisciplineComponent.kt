/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package dev.forcetower.disciplines.core.injection

import android.content.Context
import com.forcetower.uefs.core.injection.dependencies.DisciplineModuleDependencies
import dagger.BindsInstance
import dagger.Component
import dev.forcetower.disciplines.feature.DisciplineFragment
import dev.forcetower.disciplines.feature.dialog.SelectGroupDialog

@Component(
    modules = [FeatureViewModels::class],
    dependencies = [DisciplineModuleDependencies::class]
)
interface DisciplineComponent {
    @Component.Builder
    interface Builder {
        fun context(@BindsInstance context: Context): Builder
        fun dependencies(dependencies: DisciplineModuleDependencies): Builder
        fun build(): DisciplineComponent
    }

    fun inject(fragment: DisciplineFragment)
    fun inject(dialog: SelectGroupDialog)
}
