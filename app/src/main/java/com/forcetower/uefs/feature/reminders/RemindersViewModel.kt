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

package com.forcetower.uefs.feature.reminders

import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.service.Reminder
import com.forcetower.uefs.core.storage.repository.RemindersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val repository: RemindersRepository
) : ViewModel(), ReminderActions {
    val reminders by lazy { repository.getReminders() }
    var currentDeadline: Long? = null

    override fun onCheck(reminder: Reminder?) {
        Timber.d("Check the reminder ${reminder?.title}")
        if (reminder != null) {
            val next = !reminder.completed
            repository.updateReminderCompleteStatus(reminder, next)
        }
    }

    override fun onClick(reminder: Reminder?) {
        Timber.d("Clicked the reminder ${reminder?.title}")
    }

    fun createReminder(title: String, description: String?) {
        val reminder = Reminder(title = title, description = description, date = currentDeadline)
        repository.createReminder(reminder)
    }

    fun deleteReminder(reminder: Reminder) {
        repository.deleteReminder(reminder)
    }
}
