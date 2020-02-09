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

package dev.forcetower.conference.core.injection

import com.forcetower.core.injection.annotation.FeatureScope
import com.forcetower.uefs.core.injection.AppComponent
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.support.AndroidSupportInjectionModule
import dev.forcetower.conference.ConferenceActivity
import dev.forcetower.conference.core.injection.module.ConferenceDaggerModule
import dev.forcetower.conference.core.injection.module.ViewModelModule
import dev.forcetower.conference.feature.schedule.ScheduleFragment

@FeatureScope
@Component(
    modules = [
        AndroidInjectionModule::class,
        AndroidSupportInjectionModule::class,
        ConferenceDaggerModule::class,
        ViewModelModule::class
    ],
    dependencies = [AppComponent::class]
)
interface ConferenceComponent {
    fun inject(activity: ConferenceActivity)
    fun inject(fragment: ScheduleFragment)
}