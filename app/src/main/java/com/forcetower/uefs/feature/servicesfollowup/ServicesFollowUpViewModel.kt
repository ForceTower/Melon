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

package com.forcetower.uefs.feature.servicesfollowup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.sagres.database.model.SagresRequestedService
import com.forcetower.uefs.core.model.unes.ServiceRequest
import com.forcetower.uefs.core.storage.repository.ServicesFollowUpRepository
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ServicesFollowUpViewModel @Inject constructor(
    private val repository: ServicesFollowUpRepository
) : ViewModel() {

    private val _refreshing = MutableLiveData<Boolean>()
    val refreshing: LiveData<Boolean>
        get() = _refreshing

    private val _pendingServices = MediatorLiveData<Event<Resource<List<SagresRequestedService>>>>()
    val pendingServices: LiveData<Event<Resource<List<SagresRequestedService>>>>
        get() = _pendingServices

    fun onRefresh() {
        if (_refreshing.value != true) {
            _refreshing.value = true
            val data = repository.getPendingServices()
            _pendingServices.addSource(data) { change ->
                _pendingServices.value = Event(change)
                if (change.status == Status.ERROR || change.status == Status.SUCCESS) {
                    _pendingServices.removeSource(data)
                    _refreshing.value = false
                }
            }
        }
    }

    fun getRequestedServices(filter: String?): LiveData<List<ServiceRequest>> = repository.getRequestedServices(filter)
}
