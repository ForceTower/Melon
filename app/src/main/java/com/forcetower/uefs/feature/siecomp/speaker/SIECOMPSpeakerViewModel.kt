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

package com.forcetower.uefs.feature.siecomp.speaker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.siecomp.Speaker
import com.forcetower.uefs.core.storage.repository.SIECOMPRepository
import com.forcetower.uefs.feature.shared.extensions.map
import com.forcetower.uefs.feature.shared.extensions.setValueIfNew
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SIECOMPSpeakerViewModel @Inject constructor(
    private val repository: SIECOMPRepository
) : ViewModel() {
    var uriString: String? = null
    private val speakerId = MutableLiveData<Long?>()
    val access = repository.getAccess()

    private val _speaker = MediatorLiveData<Speaker?>()
    val speaker: LiveData<Speaker?>
        get() = _speaker

    val hasProfileImage: LiveData<Boolean> = _speaker.map {
        !it?.image.isNullOrBlank() && it?.image != "null"
    }

    init {
        _speaker.addSource(speakerId) {
            Timber.d("Speaked Id set to $it")
            refreshSpeaker(it)
        }
    }

    private fun refreshSpeaker(id: Long?) {
        if (id != null) {
            val source = repository.getSpeaker(id)
            _speaker.addSource(source) { value ->
                _speaker.value = value
                Timber.d("Speaker $value")
            }
        } else {
            _speaker.value = null
        }
    }

    fun setSpeakerId(id: Long?) {
        speakerId.setValueIfNew(id)
    }

    fun sendSpeaker(speaker: Speaker, create: Boolean) {
        repository.sendSpeaker(speaker, create)
    }
}
