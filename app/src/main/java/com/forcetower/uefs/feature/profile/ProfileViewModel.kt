/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.feature.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.unes.ProfileStatement
import com.forcetower.uefs.core.model.unes.SStudent
import com.forcetower.uefs.core.storage.repository.ProfileRepository
import com.forcetower.uefs.feature.shared.extensions.setValueIfNew
import timber.log.Timber
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {
    private val profileId = MutableLiveData<Long?>()

    private val _profile = MediatorLiveData<SStudent?>()
    val profile: LiveData<SStudent?>
        get() = _profile

    private val _statements = MediatorLiveData<List<ProfileStatement>>()
    val statements: LiveData<List<ProfileStatement>>
        get() = _statements

    init {
        _profile.addSource(profileId) {
            refreshProfile(it)
            refreshStatements(it)
        }
    }

    private fun refreshStatements(profileId: Long?) {
        profileId ?: return
        val source = repository.loadStatements(profileId)
        _statements.addSource(source) {
            Timber.d("Resource status ${it.status}")
            val data = it.data
            if (data != null) {
                _statements.value = data
            }
        }
    }

    private fun refreshProfile(profileId: Long?) {
        if (profileId != null) {
            val source = repository.loadProfile(profileId)
            Timber.d("Fetching profile...")
            _profile.addSource(source) {
                Timber.d("Profile load update ${it.status}")
                val data = it.data
                if (data != null) {
                    _profile.value = data
                }
            }
        } else {
            Timber.d("No profile information available")
        }
    }

    fun getMeProfile() = repository.getMeProfile()

    fun setProfileId(newProfileId: Long?) {
        Timber.d("Setting new profile id: $newProfileId")
        profileId.setValueIfNew(newProfileId)
    }
}