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

import com.forcetower.uefs.easter.darktheme.DarkThemeUnlockFragment
import com.forcetower.uefs.easter.darktheme.InviteDarkThemeFragment
import com.forcetower.uefs.feature.adventure.AdventureFragment
import com.forcetower.uefs.feature.adventure.AdventureSignInDialog
import com.forcetower.uefs.feature.bigtray.BigTrayFragment
import com.forcetower.uefs.feature.calendar.CalendarFragment
import com.forcetower.uefs.feature.disciplines.DisciplineFragment
import com.forcetower.uefs.feature.disciplines.DisciplineSemesterFragment
import com.forcetower.uefs.feature.disciplines.dialog.SelectGroupDialog
import com.forcetower.uefs.feature.document.DocumentsFragment
import com.forcetower.uefs.feature.event.EventFragment
import com.forcetower.uefs.feature.feedback.SendFeedbackFragment
import com.forcetower.uefs.feature.home.HomeBottomFragment
import com.forcetower.uefs.feature.home.InvalidAccessDialog
import com.forcetower.uefs.feature.home.LogoutConfirmationFragment
import com.forcetower.uefs.feature.mechcalculator.MechCreateDialog
import com.forcetower.uefs.feature.mechcalculator.MechanicalFragment
import com.forcetower.uefs.feature.messages.MessagesFragment
import com.forcetower.uefs.feature.messages.SagresMessagesFragment
import com.forcetower.uefs.feature.messages.UnesMessagesFragment
import com.forcetower.uefs.feature.purchases.PurchasesFragment
import com.forcetower.uefs.feature.reminders.CreateReminderDialog
import com.forcetower.uefs.feature.reminders.RemindersFragment
import com.forcetower.uefs.feature.schedule.ScheduleFragment
import com.forcetower.uefs.feature.servicesfollowup.RequestedServicesFragment
import com.forcetower.uefs.feature.servicesfollowup.ServicesFollowUpFragment
import com.forcetower.uefs.feature.setup.SelectCourseDialog
import com.forcetower.uefs.feature.syncregistry.SyncRegistryFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class HomeModule {
    @ContributesAndroidInjector
    abstract fun sagresMessageFragment(): SagresMessagesFragment
    @ContributesAndroidInjector
    abstract fun messagesFragment(): MessagesFragment
    @ContributesAndroidInjector
    abstract fun unesMessageFragment(): UnesMessagesFragment
    @ContributesAndroidInjector
    abstract fun homeBottomFragment(): HomeBottomFragment
    @ContributesAndroidInjector
    abstract fun scheduleFragment(): ScheduleFragment
    @ContributesAndroidInjector
    abstract fun disciplineFragment(): DisciplineFragment
    @ContributesAndroidInjector
    abstract fun disciplineSemesterFragment(): DisciplineSemesterFragment
    @ContributesAndroidInjector
    abstract fun bigTrayFragment(): BigTrayFragment
    @ContributesAndroidInjector
    abstract fun documentsFragment(): DocumentsFragment
    @ContributesAndroidInjector
    abstract fun syncRegistryFragment(): SyncRegistryFragment
    @ContributesAndroidInjector
    abstract fun eventFragment(): EventFragment
    @ContributesAndroidInjector
    abstract fun calendarFragment(): CalendarFragment
    @ContributesAndroidInjector
    abstract fun remindersFragment(): RemindersFragment
    @ContributesAndroidInjector
    abstract fun createReminderDialog(): CreateReminderDialog
    @ContributesAndroidInjector
    abstract fun selectGroupDialog(): SelectGroupDialog
    @ContributesAndroidInjector
    abstract fun adventureFragment(): AdventureFragment
    @ContributesAndroidInjector
    abstract fun adventureSignInDialog(): AdventureSignInDialog
    @ContributesAndroidInjector
    abstract fun purchasesFragment(): PurchasesFragment
    @ContributesAndroidInjector
    abstract fun servicesFollowUpFragment(): ServicesFollowUpFragment
    @ContributesAndroidInjector
    abstract fun requestedServicesFragment(): RequestedServicesFragment
    @ContributesAndroidInjector
    abstract fun darkThemeUnlockFragment(): DarkThemeUnlockFragment
    @ContributesAndroidInjector
    abstract fun inviteDarkThemeFragment(): InviteDarkThemeFragment
    @ContributesAndroidInjector
    abstract fun mechanicalFragment(): MechanicalFragment
    @ContributesAndroidInjector
    abstract fun mechCreateDialog(): MechCreateDialog
    @ContributesAndroidInjector
    abstract fun sendFeedbackFragment(): SendFeedbackFragment
    @ContributesAndroidInjector
    abstract fun invalidAccessDialog(): InvalidAccessDialog
    @ContributesAndroidInjector
    abstract fun logoutConfirmationFragment(): LogoutConfirmationFragment
    @ContributesAndroidInjector
    abstract fun selectCourseDialog(): SelectCourseDialog
}
