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

package dev.forcetower.conference.feature.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.conference.core.model.persistence.Session
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor() : ViewModel(), ScheduleActions {
    // TODO Implement logic
    val isLoading: LiveData<Boolean> = MutableLiveData()
    val refreshing: LiveData<Boolean> = MutableLiveData()

    fun onSwipeRefresh() {
    }

    override fun onOpenSession(session: Session) {
    }

    override fun onStarClick(session: Session) {
    }
}
