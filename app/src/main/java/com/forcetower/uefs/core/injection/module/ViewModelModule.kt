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

package com.forcetower.uefs.core.injection.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.forcetower.uefs.core.injection.annotation.ViewModelKey
import com.forcetower.uefs.core.vm.BillingViewModel
import com.forcetower.uefs.core.vm.CourseViewModel
import com.forcetower.uefs.core.vm.LaunchViewModel
import com.forcetower.uefs.core.vm.SnackbarViewModel
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.easter.darktheme.DarkThemeViewModel
import com.forcetower.uefs.feature.about.ContributorViewModel
import com.forcetower.uefs.feature.adventure.AdventureViewModel
import com.forcetower.uefs.feature.bigtray.BigTrayViewModel
import com.forcetower.uefs.feature.calendar.AcademicCalendarViewModel
import com.forcetower.uefs.feature.demand.DemandViewModel
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.document.DocumentsViewModel
import com.forcetower.uefs.feature.event.EventViewModel
import com.forcetower.uefs.feature.home.HomeViewModel
import com.forcetower.uefs.feature.login.LoginViewModel
import com.forcetower.uefs.feature.messages.MessagesViewModel
import com.forcetower.uefs.feature.profile.ProfileViewModel
import com.forcetower.uefs.feature.reminders.RemindersViewModel
import com.forcetower.uefs.feature.schedule.ScheduleViewModel
import com.forcetower.uefs.feature.servicesfollowup.ServicesFollowUpViewModel
import com.forcetower.uefs.feature.settings.SettingsViewModel
import com.forcetower.uefs.feature.setup.SetupViewModel
import com.forcetower.uefs.feature.syncregistry.SyncRegistryViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    abstract fun bindLoginViewModel(vm: LoginViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindHomeViewModel(vm: HomeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ScheduleViewModel::class)
    abstract fun bindScheduleViewModel(vm: ScheduleViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LaunchViewModel::class)
    abstract fun bindLaunchViewModel(vm: LaunchViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DisciplineViewModel::class)
    abstract fun bindDisciplineViewModel(vm: DisciplineViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfileViewModel::class)
    abstract fun bindProfileViewModel(vm: ProfileViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MessagesViewModel::class)
    abstract fun bindMessagesViewModel(vm: MessagesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CourseViewModel::class)
    abstract fun bindCourseViewModel(vm: CourseViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupViewModel::class)
    abstract fun bindSetupViewModel(vm: SetupViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BigTrayViewModel::class)
    abstract fun bindBigTrayViewModel(vm: BigTrayViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DocumentsViewModel::class)
    abstract fun bindDocumentsViewModel(vm: DocumentsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SyncRegistryViewModel::class)
    abstract fun bindSyncRegistryViewModel(vm: SyncRegistryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EventViewModel::class)
    abstract fun bindEventViewModel(vm: EventViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AcademicCalendarViewModel::class)
    abstract fun bindAcademicCalendarViewModel(vm: AcademicCalendarViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RemindersViewModel::class)
    abstract fun bindRemindersViewModel(vm: RemindersViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DemandViewModel::class)
    abstract fun bindDemandViewModel(vm: DemandViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SnackbarViewModel::class)
    abstract fun bindSnackbarViewModel(vm: SnackbarViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ContributorViewModel::class)
    abstract fun bindContributorViewModel(vm: ContributorViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AdventureViewModel::class)
    abstract fun bindAdventureViewModel(vm: AdventureViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BillingViewModel::class)
    abstract fun bindBillingViewModel(vm: BillingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ServicesFollowUpViewModel::class)
    abstract fun bindServicesFollowUpViewModel(vm: ServicesFollowUpViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun bindSettingsViewModel(vm: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DarkThemeViewModel::class)
    abstract fun bindDarkThemeViewModel(vm: DarkThemeViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: UViewModelFactory): ViewModelProvider.Factory
}