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

abstract class NetworkBoundResource<ResultType, RequestType>
@MainThread constructor(private val executors: AppExecutors) {
    private val result = MediatorLiveData<Resource<ResultType>>()

    init {
        result.postValue(Resource.loading(null))
        @Suppress("LeakingThis")
        val dbSource = loadFromDb()
        result.addSource(dbSource) {
            result.removeSource(dbSource)
            if (shouldFetch(it)) {
                fetchFromNetwork(dbSource)
            } else {
                result.addSource(dbSource) { data ->
                    setValue(Resource.success(data))
                }
            }
        }
    }

    private fun fetchFromNetwork(dbSource: LiveData<ResultType>) {
        val apiResponse = createCall()
        result.addSource(dbSource) { newData ->
            setValue(Resource.loading(newData))
        }
        result.addSource(apiResponse) { response ->
            result.removeSource(apiResponse)
            result.removeSource(dbSource)
            Timber.d("Received response: ${response.javaClass}")
            when (response) {
                is ApiSuccessResponse -> {
                    executors.diskIO().execute {
                        saveCallResult(processResponse(response))
                        executors.mainThread().execute {
                            result.addSource(loadFromDb()) { newData ->
                                setValue(Resource.success(newData))
                            }
                        }
                    }
                }
                is ApiEmptyResponse -> {
                    executors.mainThread().execute {
                        result.addSource(loadFromDb()) { newData ->
                            setValue(Resource.success(newData))
                        }
                    }
                }
                is ApiErrorResponse -> {
                    executors.diskIO().execute {
                        onErrorCallback()
                        executors.mainThread().execute {
                            result.addSource(dbSource) { newData ->
                                setValue(Resource.error(response.errorMessage, newData))
                            }
                        }
                    }
                }
            }
        }
    }

    @MainThread
    private fun setValue(newValue: Resource<ResultType>) {
        if (result.value != newValue) {
            result.value = newValue
        }
    }

    @WorkerThread
    protected open fun processResponse(response: ApiSuccessResponse<RequestType>) = response.body

    fun asLiveData() = result as LiveData<Resource<ResultType>>

    @MainThread
    abstract fun loadFromDb(): LiveData<ResultType>
    @MainThread
    abstract fun shouldFetch(it: ResultType?): Boolean
    @MainThread
    abstract fun createCall(): LiveData<ApiResponse<RequestType>>
    @WorkerThread
    abstract fun saveCallResult(value: RequestType)
    @WorkerThread
    open fun onErrorCallback() = Unit
}
