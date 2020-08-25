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

import com.forcetower.uefs.feature.disciplines.dialog.SelectMaterialDialog
import com.forcetower.uefs.feature.disciplines.disciplinedetail.classes.ClassesFragment
import com.forcetower.uefs.feature.disciplines.disciplinedetail.DisciplineDetailsFragment
import com.forcetower.uefs.feature.disciplines.disciplinedetail.GradesFragment
import com.forcetower.uefs.feature.disciplines.disciplinedetail.absences.AbsencesFragment
import com.forcetower.uefs.feature.disciplines.disciplinedetail.materials.MaterialsFragment
import com.forcetower.uefs.feature.disciplines.disciplinedetail.overview.OverviewFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class DisciplineModule {
    @ContributesAndroidInjector
    abstract fun overviewFragment(): OverviewFragment
    @ContributesAndroidInjector
    abstract fun disciplineDetailsFragment(): DisciplineDetailsFragment
    @ContributesAndroidInjector
    abstract fun materialsFragment(): MaterialsFragment
    @ContributesAndroidInjector
    abstract fun classesFragment(): ClassesFragment
    @ContributesAndroidInjector
    abstract fun gradesFragment(): GradesFragment
    @ContributesAndroidInjector
    abstract fun absencesFragment(): AbsencesFragment
    @ContributesAndroidInjector
    abstract fun selectMaterialDialog(): SelectMaterialDialog
}
