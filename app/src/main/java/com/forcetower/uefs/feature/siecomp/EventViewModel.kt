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

package com.forcetower.uefs.feature.siecomp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.R
import com.forcetower.uefs.core.storage.eventdatabase.accessors.SessionWithData
import com.forcetower.uefs.core.storage.repository.SIECOMPRepository
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.vm.Event
import com.forcetower.uefs.feature.siecomp.common.SessionActions
import javax.inject.Inject

class EventViewModel @Inject constructor(
    private val repository: SIECOMPRepository
) : ViewModel(), SessionActions {
    var sessionsLoaded: Boolean = false
    private var loading: Boolean = false

    val refreshing: MutableLiveData<Boolean> = MutableLiveData()
    val refreshSource: MediatorLiveData<Resource<List<SessionWithData>>> = MediatorLiveData()

    private val _navigateToSessionAction = MutableLiveData<Event<Long>>()
    val navigateToSessionAction: LiveData<Event<Long>>
        get() = _navigateToSessionAction

    private val _snackbarMessenger = MutableLiveData<Event<Int>>()
    val snackbarMessenger: LiveData<Event<Int>>
        get() = _snackbarMessenger

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

    override fun openSessionDetails(id: Long) {
        _navigateToSessionAction.value = Event(id)
    }

    override fun onStarClicked(session: SessionWithData) {
        val newIsStarredState = !session.isStarred()

        val stringResId = if (newIsStarredState) {
            R.string.event_starred
        } else {
            R.string.event_unstarred
        }

        _snackbarMessenger.value = Event(stringResId)

        // TODO
        // repository.markSessionStar(session.session.uid, newIsStarredState)
    }
}