/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.core.task.successOr
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.unes.usecases.auth.HasEnrolledAccessUseCase
import dev.forcetower.unes.usecases.version.NotifyNewVersionUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LaunchViewModel @Inject constructor(
    private val hasEnrolledAccess: HasEnrolledAccessUseCase,
    private val notifyNewVersion: NotifyNewVersionUseCase
) : ViewModel() {
    private val _direction = MutableLiveData<Event<Destination>>()
    val direction: LiveData<Event<Destination>> = _direction

    fun findStarterDirection() {
        viewModelScope.launch {
            val connected = hasEnrolledAccess(Unit).successOr(false)
            _direction.value = if (connected) {
                Event(Destination.HOME_ACTIVITY)
            } else {
                Event(Destination.LOGIN_ACTIVITY)
            }
        }
    }

    fun checkNewAppVersion() {
        viewModelScope.launch {
            notifyNewVersion(Unit)
        }
    }

    enum class Destination { LOGIN_ACTIVITY, HOME_ACTIVITY }
}
