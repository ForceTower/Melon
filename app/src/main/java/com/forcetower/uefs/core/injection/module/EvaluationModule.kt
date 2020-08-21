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

import com.forcetower.uefs.feature.evaluation.InitialFragment
import com.forcetower.uefs.feature.evaluation.PresentationFragment
import com.forcetower.uefs.feature.evaluation.discipline.DisciplineEvaluationFragment
import com.forcetower.uefs.feature.evaluation.home.HomeFragment
import com.forcetower.uefs.feature.evaluation.rating.RatingFragment
import com.forcetower.uefs.feature.evaluation.search.SearchFragment
import com.forcetower.uefs.feature.evaluation.teacher.TeacherFragment
import com.forcetower.uefs.feature.universe.UnesverseRequiredFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class EvaluationModule {
    @ContributesAndroidInjector
    abstract fun initial(): InitialFragment
    @ContributesAndroidInjector
    abstract fun presentation(): PresentationFragment
    @ContributesAndroidInjector
    abstract fun unesverseRequired(): UnesverseRequiredFragment
    @ContributesAndroidInjector
    abstract fun home(): HomeFragment
    @ContributesAndroidInjector
    abstract fun search(): SearchFragment
    @ContributesAndroidInjector
    abstract fun teacher(): TeacherFragment
    @ContributesAndroidInjector
    abstract fun discipline(): DisciplineEvaluationFragment
    @ContributesAndroidInjector
    abstract fun rating(): RatingFragment
}