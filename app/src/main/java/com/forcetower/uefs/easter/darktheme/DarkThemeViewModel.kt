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

package com.forcetower.uefs.easter.darktheme

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.storage.repository.AccountRepository
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DarkThemeViewModel @Inject constructor(
    private val repository: DarkThemeRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    val preconditions: LiveData<List<Precondition>>
        get() = repository.getPreconditions()

    val profile: LiveData<Account?>
        get() = accountRepository.getAccountOnDatabase()

    private val _currentCall = MediatorLiveData<Event<Resource<Boolean>>>()
    val currentCall: LiveData<Event<Resource<Boolean>>>
        get() = _currentCall

    fun sendDarkThemeTo(username: String?) {
        val source = repository.sendDarkThemeTo(username)
        _currentCall.addSource(source) {
            if (it.status == Status.SUCCESS || it.status == Status.ERROR) {
                _currentCall.removeSource(source)
            }
            _currentCall.value = Event(it)
        }
    }
}
