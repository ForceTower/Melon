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

package com.forcetower.uefs.feature.siecomp.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.core.model.siecomp.Session
import com.forcetower.uefs.core.model.siecomp.Speaker
import com.forcetower.uefs.core.model.siecomp.Tag
import com.forcetower.uefs.core.storage.repository.SIECOMPRepository
import com.forcetower.uefs.feature.shared.SetIntervalLiveData
import com.forcetower.uefs.feature.shared.extensions.map
import com.forcetower.uefs.feature.shared.extensions.setValueIfNew
import com.forcetower.uefs.feature.siecomp.common.SpeakerActions
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

private const val TEN_SECONDS = 10_000L

@HiltViewModel
class SIECOMPSessionViewModel @Inject constructor(
    private val repository: SIECOMPRepository
) : ViewModel(), SpeakerActions {
    private val sessionId = MutableLiveData<Long?>()

    private val _session = MediatorLiveData<Session?>()
    val session: LiveData<Session?>
        get() = _session

    private val _tags = MutableLiveData<List<Tag>>()
    val tags: LiveData<List<Tag>>
        get() = _tags

    private val _speakers = MutableLiveData<List<Speaker>>()
    val speakers: LiveData<List<Speaker>>
        get() = _speakers

    private val _navigateToSpeakerAction = MutableLiveData<Event<Long>>()
    val navigateToSpeakerAction: LiveData<Event<Long>>
        get() = _navigateToSpeakerAction

    val hasPhoto: LiveData<Boolean>
    val timeUntilStart: LiveData<Duration?>

    val timeZoneId: ZoneId = ZoneId.systemDefault()

    init {
        _session.addSource(sessionId) {
            Timber.d("Session set to ID: $it")
            refreshSession(it)
        }

        hasPhoto = session.map {
            !it?.photoUrl.isNullOrBlank() && it?.photoUrl != "null"
        }

        timeUntilStart = SetIntervalLiveData.DefaultIntervalMapper.mapAtInterval(session, TEN_SECONDS) { session ->
            session?.startTime?.let { startTime ->
                val duration = Duration.between(Instant.now(), startTime)
                when (duration.toMinutes()) {
                    in 1..30 -> duration
                    else -> null
                }
            }
        }
    }

    private fun refreshSession(value: Long?) {
        if (value != null) {
            // TODO Should attempt to fetch from network
            // [In this case it will work since all data is already on database]
            _session.addSource(repository.getSessionDetails(value)) {
                _session.value = it.session
                _tags.value = it.tags()
                _speakers.value = it.speakers()
            }
        }
    }

    fun setSessionId(id: Long?) {
        sessionId.setValueIfNew(id)
    }

    override fun onSpeakerClicked(id: Long) {
        _navigateToSpeakerAction.value = Event(id)
    }
}
