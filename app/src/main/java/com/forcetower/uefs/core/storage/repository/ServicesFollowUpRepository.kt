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

package com.forcetower.uefs.core.storage.repository

import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SagresRequestedService
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
    fun getPendingServices(): LiveData<Resource<List<SagresRequestedService>>> {
        val result = MutableLiveData<Resource<List<SagresRequestedService>>>()
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
        return when {
            filter == null -> database.serviceRequestDao().getAll()
            filter.equals("incomplete", ignoreCase = true) -> database.serviceRequestDao().getIncomplete()
            filter.equals("complete", ignoreCase = true) -> database.serviceRequestDao().getComplete()
            else -> database.serviceRequestDao().getFiltered(filter)
        }
    }
}
