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

package com.forcetower.uefs.core.injection

import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.injection.module.ActivityModule
import com.forcetower.uefs.core.injection.module.AppModule
import com.forcetower.uefs.core.injection.module.FirebaseCoreModule
import com.forcetower.uefs.core.injection.module.FirestoreModule
import com.forcetower.uefs.core.injection.module.NetworkModule
import com.forcetower.uefs.core.injection.module.ReceiverModule
import com.forcetower.uefs.core.injection.module.ServicesModule
import com.forcetower.uefs.core.injection.module.ViewModelModule
import com.forcetower.uefs.core.work.demand.CreateDemandWorker
import com.forcetower.uefs.core.work.discipline.DisciplinesDetailsWorker
import com.forcetower.uefs.core.work.grades.GradesSagresWorker
import com.forcetower.uefs.core.work.hourglass.HourglassContributeWorker
import com.forcetower.uefs.core.work.image.UploadImageToStorage
import com.forcetower.uefs.core.work.sync.SyncLinkedWorker
import com.forcetower.uefs.core.work.sync.SyncMainWorker
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

/**
 * Define todas as regras de injeção de dependencias.
 * Onde, como, quando e por quem.
 */
@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        AndroidSupportInjectionModule::class,
        AppModule::class,
        ReceiverModule::class,
        NetworkModule::class,
        FirebaseCoreModule::class,
        FirestoreModule::class,
        ServicesModule::class,
        ViewModelModule::class,
        ActivityModule::class
    ]
)
interface AppComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance fun application(app: UApplication): Builder
        fun build(): AppComponent
    }

    fun inject(app: UApplication)
    fun inject(worker: SyncMainWorker)
    fun inject(worker: SyncLinkedWorker)
    fun inject(worker: GradesSagresWorker)
    fun inject(worker: CreateDemandWorker)
    fun inject(worker: HourglassContributeWorker)
    fun inject(worker: DisciplinesDetailsWorker)
    fun inject(worker: UploadImageToStorage)
}