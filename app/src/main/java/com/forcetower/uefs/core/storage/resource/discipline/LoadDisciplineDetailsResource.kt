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

package com.forcetower.uefs.core.storage.resource.discipline

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MediatorLiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.operation.disciplines.FastDisciplinesCallback
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.util.toLiveData
import timber.log.Timber

abstract class LoadDisciplineDetailsResource @MainThread constructor(
    private val executors: AppExecutors,
    private val database: UDatabase,
    private val semester: String? = null,
    private val code: String? = null,
    private val group: String? = null,
    private val partialLoad: Boolean = false,
    private val discover: Boolean = false
) {
    private val result = MediatorLiveData<FastDisciplinesCallback>()

    init {
        loginToSagres()
    }

    @MainThread
    private fun loginToSagres() {
        val access = database.accessDao().getAccess()
        result.addSource(access) { data ->
            result.removeSource(access)
            if (data == null) {
                result.value = FastDisciplinesCallback(Status.INVALID_LOGIN).flags(FastDisciplinesCallback.LOGIN)
            } else {
                val login = SagresNavigator.instance.aLogin(data.username, data.password).toLiveData()
                result.addSource(login) {
                    result.removeSource(login)
                    loadFromSagres()
                }
            }
        }
    }

    @MainThread
    private fun loadFromSagres() {
        val loader = SagresNavigator.instance.aDisciplinesExperimental(semester, code, group, partialLoad, discover).toLiveData()
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
    private fun startSaveResults(callback: FastDisciplinesCallback) {
        result.postValue(FastDisciplinesCallback(Status.LOADING).flags(FastDisciplinesCallback.SAVING))
        executors.diskIO().execute {
            saveResults(callback)
            result.postValue(FastDisciplinesCallback(Status.LOADING).flags(FastDisciplinesCallback.GRADES))
            loadGrades()
            result.postValue(callback)
        }
    }

    @WorkerThread
    abstract fun loadGrades()

    @WorkerThread
    abstract fun saveResults(callback: FastDisciplinesCallback)

    fun asLiveData() = result
}
