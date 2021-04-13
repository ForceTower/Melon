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

package dev.forcetower.event.feature.listing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forcetower.uefs.core.model.unes.Event
import dev.forcetower.event.core.repository.EventRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

typealias SingleEventAction = com.forcetower.core.lifecycle.Event<Event>

class EventViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel(), EventActions {
    private val _onEventClicked = MutableLiveData<SingleEventAction>()
    val onEventClicked: LiveData<SingleEventAction>
        get() = _onEventClicked

    val events = repository.events

    override fun onEventClick(event: Event) {
        _onEventClicked.value = SingleEventAction(event)
    }

    override fun approve(event: Event) {
        viewModelScope.launch {
            repository.approve(event.id)
        }
    }

    override fun delete(event: Event) {
        viewModelScope.launch {
            repository.delete(event.id)
        }
    }
}
