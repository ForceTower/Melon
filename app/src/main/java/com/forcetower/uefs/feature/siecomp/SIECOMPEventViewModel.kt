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

package com.forcetower.uefs.feature.siecomp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.R
import com.forcetower.uefs.core.storage.eventdatabase.accessors.SessionWithData
import com.forcetower.uefs.core.storage.repository.SIECOMPRepository
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.feature.siecomp.common.SessionActions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SIECOMPEventViewModel @Inject constructor(
    private val repository: SIECOMPRepository
) : ViewModel(), SessionActions {
    var sessionsLoaded: Boolean = false
    private var loading: Boolean = false

    val access = repository.getAccess()

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
        repository.markSessionStar(session.session.uid, newIsStarredState)
    }

    fun loginToService(user: String, pass: String) {
        repository.loginToService(user, pass)
    }
}
