/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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