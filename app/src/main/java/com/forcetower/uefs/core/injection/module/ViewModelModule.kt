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

package com.forcetower.uefs.core.injection.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.forcetower.core.injection.annotation.ViewModelKey
import com.forcetower.uefs.core.vm.BillingViewModel
import com.forcetower.uefs.core.vm.CourseViewModel
import com.forcetower.uefs.core.vm.LaunchViewModel
import com.forcetower.uefs.core.vm.SnackbarViewModel
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.core.vm.UnesverseViewModel
import com.forcetower.uefs.core.vm.UserSessionViewModel
import com.forcetower.uefs.easter.darktheme.DarkThemeViewModel
import com.forcetower.uefs.feature.about.ContributorViewModel
import com.forcetower.uefs.feature.adventure.AdventureViewModel
import com.forcetower.uefs.feature.bigtray.BigTrayViewModel
import com.forcetower.uefs.feature.calendar.AcademicCalendarViewModel
import com.forcetower.uefs.feature.demand.DemandViewModel
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.document.DocumentsViewModel
import com.forcetower.uefs.feature.evaluation.EvaluationViewModel
import com.forcetower.uefs.feature.evaluation.rating.EvaluationRatingViewModel
import com.forcetower.uefs.feature.event.EventViewModel
import com.forcetower.uefs.feature.feedback.FeedbackViewModel
import com.forcetower.uefs.feature.flowchart.FlowchartViewModel
import com.forcetower.uefs.feature.forms.FormsViewModel
import com.forcetower.uefs.feature.home.HomeViewModel
import com.forcetower.uefs.feature.login.LoginViewModel
import com.forcetower.uefs.feature.mechcalculator.MechanicalViewModel
import com.forcetower.uefs.feature.messages.MessagesDFMViewModel
import com.forcetower.uefs.feature.messages.MessagesViewModel
import com.forcetower.uefs.feature.profile.ProfileViewModel
import com.forcetower.uefs.feature.reminders.RemindersViewModel
import com.forcetower.uefs.feature.schedule.ScheduleViewModel
import com.forcetower.uefs.feature.servicesfollowup.ServicesFollowUpViewModel
import com.forcetower.uefs.feature.settings.SettingsViewModel
import com.forcetower.uefs.feature.setup.SetupViewModel
import com.forcetower.uefs.feature.siecomp.SIECOMPEventViewModel
import com.forcetower.uefs.feature.siecomp.onboarding.OnboardingViewModel
import com.forcetower.uefs.feature.siecomp.session.SIECOMPSessionViewModel
import com.forcetower.uefs.feature.siecomp.speaker.SIECOMPSpeakerViewModel
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
    @IntoMap
    @ViewModelKey(MechanicalViewModel::class)
    abstract fun bindMechanicalViewModel(vm: MechanicalViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FeedbackViewModel::class)
    abstract fun bindFeedbackViewModel(vm: FeedbackViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(OnboardingViewModel::class)
    abstract fun bindOnboardingViewModel(vm: OnboardingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SIECOMPEventViewModel::class)
    abstract fun bindSIECOMPEventViewModel(vm: SIECOMPEventViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SIECOMPSessionViewModel::class)
    abstract fun bindSIECOMPSessionViewModel(vm: SIECOMPSessionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SIECOMPSpeakerViewModel::class)
    abstract fun bindSIECOMPSpeakerViewModel(vm: SIECOMPSpeakerViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EvaluationViewModel::class)
    abstract fun bindEvaluationViewModel(vm: EvaluationViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EvaluationRatingViewModel::class)
    abstract fun bindEvaluationRatingViewModel(vm: EvaluationRatingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UnesverseViewModel::class)
    abstract fun bindUnesverseViewModel(vm: UnesverseViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FlowchartViewModel::class)
    abstract fun bindFlowchartViewModel(vm: FlowchartViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UserSessionViewModel::class)
    abstract fun bindUserSessionViewModel(vm: UserSessionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FormsViewModel::class)
    abstract fun bindFormsViewModel(vm: FormsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MessagesDFMViewModel::class)
    abstract fun bindMessagesDFMViewModel(vm: MessagesDFMViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: UViewModelFactory): ViewModelProvider.Factory
}