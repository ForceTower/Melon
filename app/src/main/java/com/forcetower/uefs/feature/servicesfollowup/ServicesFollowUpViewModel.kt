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

package com.forcetower.uefs.feature.servicesfollowup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.sagres.database.model.SRequestedService
import com.forcetower.uefs.core.model.unes.ServiceRequest
import com.forcetower.uefs.core.storage.repository.ServicesFollowUpRepository
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.vm.Event
import javax.inject.Inject

class ServicesFollowUpViewModel @Inject constructor(
    private val repository: ServicesFollowUpRepository
) : ViewModel() {

    private val _refreshing = MutableLiveData<Boolean>()
    val refreshing: LiveData<Boolean>
        get() = _refreshing

    private val _pendingServices = MediatorLiveData<Event<Resource<List<SRequestedService>>>>()
    val pendingServices: LiveData<Event<Resource<List<SRequestedService>>>>
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