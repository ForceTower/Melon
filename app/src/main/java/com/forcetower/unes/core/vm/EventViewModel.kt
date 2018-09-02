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

package com.forcetower.unes.core.vm

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.unes.core.storage.database.accessors.SessionWithData
import com.forcetower.unes.core.storage.repository.SIECOMPRepository
import com.forcetower.unes.core.storage.resource.Resource
import com.forcetower.unes.core.storage.resource.Status
import timber.log.Timber
import javax.inject.Inject

class EventViewModel @Inject constructor(
    private val repository: SIECOMPRepository
): ViewModel() {
    private var loading: Boolean = false

    val refreshing: MutableLiveData<Boolean> = MutableLiveData()
    val refreshSource: MediatorLiveData<Resource<List<SessionWithData>>> = MediatorLiveData()

    fun getSessionsFromDayLocal(day: Int) = repository.getSessionsFromDayLocal(day)

    fun loadSessions() {
        if (!loading) {
            loading = true
            val source = repository.getAllSessions()
            refreshSource.addSource(source) {
                when (it.status) {
                    Status.SUCCESS, Status.ERROR -> {
                        refreshSource.removeSource(source)
                        refreshing.value = false
                        loading = false
                    }
                    Status.LOADING -> {
                        refreshing.value = true
                        loading = true
                    }
                }
                refreshSource.value = it
            }
        }
    }
}