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

package com.forcetower.uefs.core.storage.repository

import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SRequestedService
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.unes.ServiceRequest
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.resource.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServicesFollowUpRepository @Inject constructor(
    private val database: UDatabase,
    private val executors: AppExecutors
) {
    // TODO change the type
    @AnyThread
    fun getPendingServices(): LiveData<Resource<List<SRequestedService>>> {
        val result = MutableLiveData<Resource<List<SRequestedService>>>()
        executors.networkIO().execute {
            val callback = SagresNavigator.instance.getRequestedServices()
            val resource = when (callback.status) {
                Status.SUCCESS -> {
                    val list = callback.services.map { ServiceRequest.fromSagres(it) }
                    database.serviceRequestDao().insertList(list)
                    database.serviceRequestDao().markAllNotified()
                    Resource.success(callback.services)
                }
                else -> Resource.error("Failed to load", callback.services)
            }
            result.postValue(resource)
        }
        return result
    }

    fun getRequestedServices(filter: String?): LiveData<List<ServiceRequest>> {
        return if (filter == null) {
            database.serviceRequestDao().getAll()
        } else if (filter.equals("incomplete", ignoreCase = true)) {
            database.serviceRequestDao().getIncomplete()
        } else {
            database.serviceRequestDao().getFiltered(filter)
        }
    }
}
