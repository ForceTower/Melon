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

package com.forcetower.uefs.core.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.feature.shared.extensions.setValueIfNew
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LaunchViewModel @Inject constructor(
    database: UDatabase
) : ViewModel() {
    var started = false
    private val accessSrc = database.accessDao().getAccess()

    private val _direction = MediatorLiveData<Event<Destination>>()
    val direction: LiveData<Event<Destination>>
        get() = _direction

    init {
        _direction.addSource(accessSrc) {
            val destination = if (it != null) Destination.HOME_ACTIVITY else Destination.LOGIN_ACTIVITY
            _direction.setValueIfNew(Event(destination))
        }
    }
}

enum class Destination { LOGIN_ACTIVITY, HOME_ACTIVITY }
