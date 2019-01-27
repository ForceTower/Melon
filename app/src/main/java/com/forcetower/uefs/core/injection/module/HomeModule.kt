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
import com.forcetower.uefs.feature.home.HomeBottomFragment
import com.forcetower.uefs.feature.messages.MessagesFragment
import com.forcetower.uefs.feature.messages.SagresMessagesFragment
import com.forcetower.uefs.feature.messages.UnesMessagesFragment
import com.forcetower.uefs.feature.purchases.PurchasesFragment
import com.forcetower.uefs.feature.reminders.CreateReminderDialog
import com.forcetower.uefs.feature.reminders.RemindersFragment
import com.forcetower.uefs.feature.schedule.ScheduleFragment
import com.forcetower.uefs.feature.servicesfollowup.RequestedServicesFragment
import com.forcetower.uefs.feature.servicesfollowup.ServicesFollowUpFragment
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
}
