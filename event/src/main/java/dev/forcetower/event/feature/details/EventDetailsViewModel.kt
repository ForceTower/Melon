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

package dev.forcetower.event.feature.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.unes.Event
import dev.forcetower.event.core.repository.EventRepository
import dev.forcetower.event.feature.listing.SingleEventAction
import javax.inject.Inject

class EventDetailsViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel(), EventDetailsActions {
    private val _eventCreationSent = MutableLiveData<SingleEventAction>()
    val onEventCreationSent: LiveData<SingleEventAction>
        get() = _eventCreationSent

    private val _eventMoveToPage = MutableLiveData<SingleEventAction>()
    val onEventMoveToPage: LiveData<SingleEventAction>
        get() = _eventMoveToPage

    fun loadModel(id: Long) = repository.getEvent(id)

    override fun onConfirmCreation(event: Event) {
        _eventCreationSent.value = SingleEventAction(event)
    }

    override fun onMoveToPage(event: Event) {
        _eventMoveToPage.value = SingleEventAction(event)
    }
}
