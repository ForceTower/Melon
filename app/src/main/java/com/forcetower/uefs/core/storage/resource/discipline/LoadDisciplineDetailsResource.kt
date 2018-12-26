/*
 * Copyright (c) 2018.
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

package com.forcetower.uefs.core.storage.resource.discipline

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MediatorLiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SDisciplineGroup
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback
import com.forcetower.uefs.AppExecutors
import timber.log.Timber

abstract class LoadDisciplineDetailsResource @MainThread constructor(
    private val executors: AppExecutors,
    private val semester: String? = null,
    private val code: String? = null,
    private val group: String? = null
) {
    private val result = MediatorLiveData<DisciplineDetailsCallback>()

    init {
        loadFromSagres()
    }

    @MainThread
    private fun loadFromSagres() {
        val loader = SagresNavigator.instance.aLoadDisciplineDetails(semester, code, group)
        result.addSource(loader) { callback ->
            Timber.d("Current Status: ${callback.status}")
            when (callback.status) {
                Status.COMPLETED -> {
                    result.removeSource(loader)
                    startSaveResults(callback)
                }
                else -> result.value = callback
            }
        }
    }

    @MainThread
    private fun startSaveResults(callback: DisciplineDetailsCallback) {
        val groups = callback.getGroups() ?: emptyList()
        result.postValue(DisciplineDetailsCallback(Status.LOADING).flags(DisciplineDetailsCallback.SAVING))
        executors.diskIO().execute {
            saveResults(groups)
            result.postValue(callback)
        }
    }

    @WorkerThread
    abstract fun saveResults(groups: List<SDisciplineGroup>)

    fun asLiveData() = result
}