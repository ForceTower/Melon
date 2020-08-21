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

import com.forcetower.uefs.LauncherActivity
import com.forcetower.uefs.core.injection.module.siecomp.SIECOMPEditorModule
import com.forcetower.uefs.core.injection.module.siecomp.SIECOMPOnboardingModule
import com.forcetower.uefs.core.injection.module.siecomp.SIECOMPScheduleModule
import com.forcetower.uefs.core.injection.module.siecomp.SIECOMPSessionModule
import com.forcetower.uefs.core.injection.module.siecomp.SIECOMPSpeakerModule
import com.forcetower.uefs.easter.twofoureight.Game2048Activity
import com.forcetower.uefs.feature.about.AboutActivity
import com.forcetower.uefs.feature.demand.DemandActivity
import com.forcetower.uefs.feature.disciplines.disciplinedetail.DisciplineDetailsActivity
import com.forcetower.uefs.feature.evaluation.EvaluationActivity
import com.forcetower.uefs.feature.evaluation.rating.RatingActivity
import com.forcetower.uefs.feature.flowchart.FlowchartActivity
import com.forcetower.uefs.feature.forms.FormActivity
import com.forcetower.uefs.feature.home.HomeActivity
import com.forcetower.uefs.feature.login.LoginActivity
import com.forcetower.uefs.feature.profile.ProfileActivity
import com.forcetower.uefs.feature.reminders.RemindersActivity
import com.forcetower.uefs.feature.settings.SettingsActivity
import com.forcetower.uefs.feature.setup.SetupActivity
import com.forcetower.uefs.feature.siecomp.editor.SIECOMPEditorActivity
import com.forcetower.uefs.feature.siecomp.onboarding.OnboardingActivity
import com.forcetower.uefs.feature.siecomp.schedule.EventScheduleActivity
import com.forcetower.uefs.feature.siecomp.session.EventSessionDetailsActivity
import com.forcetower.uefs.feature.siecomp.speaker.EventSpeakerActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityModule {
    @ContributesAndroidInjector
    abstract fun bindLauncherActivity(): LauncherActivity
    @ContributesAndroidInjector(modules = [LoginModule::class])
    abstract fun bindLoginActivity(): LoginActivity
    @ContributesAndroidInjector(modules = [SetupModule::class])
    abstract fun bindSetupActivity(): SetupActivity
    @ContributesAndroidInjector(modules = [HomeModule::class])
    abstract fun bindHomeActivity(): HomeActivity
    @ContributesAndroidInjector(modules = [AboutModule::class])
    abstract fun bindAboutActivity(): AboutActivity
    @ContributesAndroidInjector(modules = [DisciplineModule::class])
    abstract fun bindDisciplineDetailsActivity(): DisciplineDetailsActivity
    @ContributesAndroidInjector(modules = [ProfileModule::class])
    abstract fun bindProfileActivity(): ProfileActivity
    @ContributesAndroidInjector(modules = [RemindersModule::class])
    abstract fun bindRemindersActivity(): RemindersActivity
    @ContributesAndroidInjector(modules = [SettingsModule::class])
    abstract fun bindSettingsActivity(): SettingsActivity
    @ContributesAndroidInjector(modules = [DemandModule::class])
    abstract fun bindDemandActivity(): DemandActivity
    @ContributesAndroidInjector(modules = [EvaluationModule::class])
    abstract fun bindEvaluationActivity(): EvaluationActivity
    @ContributesAndroidInjector(modules = [RatingModule::class])
    abstract fun bindRatingActivity(): RatingActivity
    @ContributesAndroidInjector(modules = [FlowchartModule::class])
    abstract fun bindFlowchartActivity(): FlowchartActivity
    @ContributesAndroidInjector(modules = [FormsModule::class])
    abstract fun bindFormActivity(): FormActivity
    @ContributesAndroidInjector(modules = [Game2048Module::class])
    abstract fun bindGame2048Activity(): Game2048Activity
    @ContributesAndroidInjector(modules = [SIECOMPOnboardingModule::class])
    abstract fun bindSIECOMPOnboardingActivity(): OnboardingActivity
    @ContributesAndroidInjector(modules = [SIECOMPScheduleModule::class])
    abstract fun bindSIECOMPScheduleActivity(): EventScheduleActivity
    @ContributesAndroidInjector(modules = [SIECOMPSessionModule::class])
    abstract fun bindSessionDetailsActivity(): EventSessionDetailsActivity
    @ContributesAndroidInjector(modules = [SIECOMPSpeakerModule::class])
    abstract fun bindSpeakerActivity(): EventSpeakerActivity
    @ContributesAndroidInjector(modules = [SIECOMPEditorModule::class])
    abstract fun bindEventEditorActivity(): SIECOMPEditorActivity
}