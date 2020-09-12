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

package com.forcetower.uefs.core.storage.resource

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.storage.network.adapter.ApiEmptyResponse
import com.forcetower.uefs.core.storage.network.adapter.ApiErrorResponse
import com.forcetower.uefs.core.storage.network.adapter.ApiResponse
import com.forcetower.uefs.core.storage.network.adapter.ApiSuccessResponse
import timber.log.Timber

abstract class NetworkOnlyResource<RequestType>
@MainThread constructor(private val executors: AppExecutors) {
    private val result = MediatorLiveData<Resource<RequestType>>()

    init {
        result.postValue(Resource.loading(null))
        @Suppress("LeakingThis")
        fetchFromNetwork()
    }

    private fun fetchFromNetwork() {
        val apiResponse = createCall()
        result.addSource(apiResponse) { response ->
            result.removeSource(apiResponse)
            Timber.d("Received response: ${response.javaClass}")
            when (response) {
                is ApiSuccessResponse -> {
                    executors.diskIO().execute {
                        saveCallResult(processResponse(response))
                        executors.mainThread().execute {
                            setValue(Resource.success(processResponse(response)))
                        }
                    }
                }
                is ApiEmptyResponse -> {
                    setValue(Resource.error("empty response", null))
                }
                is ApiErrorResponse -> {
                    setValue(Resource.error(response.errorMessage, null))
                }
            }
        }
    }

    @MainThread
    private fun setValue(newValue: Resource<RequestType>) {
        if (result.value != newValue) {
            result.value = newValue
        }
    }

    @WorkerThread
    protected open fun processResponse(response: ApiSuccessResponse<RequestType>) = response.body

    fun asLiveData() = result as LiveData<Resource<RequestType>>

    @MainThread
    abstract fun createCall(): LiveData<ApiResponse<RequestType>>
    @WorkerThread
    abstract fun saveCallResult(value: RequestType)
}
