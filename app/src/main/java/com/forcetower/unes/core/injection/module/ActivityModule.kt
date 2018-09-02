/*
 * Copyright (c) 2018.
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

package com.forcetower.unes.core.injection.module

import com.forcetower.unes.LauncherActivity
import com.forcetower.unes.feature.about.AboutActivity
import com.forcetower.unes.feature.home.HomeActivity
import com.forcetower.unes.feature.login.LoginActivity
import com.forcetower.unes.feature.siecomp.SiecompActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityModule {
    @ContributesAndroidInjector(modules = [LoginModule::class])
    abstract fun bindLoginActivity(): LoginActivity
    @ContributesAndroidInjector(modules = [HomeModule::class])
    abstract fun bindHomeActivity() : HomeActivity
    @ContributesAndroidInjector(modules = [AboutModule::class])
    abstract fun bindAboutActivity(): AboutActivity
    @ContributesAndroidInjector(modules = [SiecompModule::class])
    abstract fun bindSiecompActivity(): SiecompActivity
    @ContributesAndroidInjector
    abstract fun bindLauncherActivity(): LauncherActivity
}