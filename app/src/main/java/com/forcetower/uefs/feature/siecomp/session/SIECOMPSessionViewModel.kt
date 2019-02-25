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

package com.forcetower.uefs.feature.siecomp.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.siecomp.Session
import com.forcetower.uefs.core.model.siecomp.Speaker
import com.forcetower.uefs.core.model.siecomp.Tag
import com.forcetower.uefs.core.storage.repository.SIECOMPRepository
import com.forcetower.uefs.core.vm.Event
import com.forcetower.uefs.feature.shared.SetIntervalLiveData
import com.forcetower.uefs.feature.shared.extensions.map
import com.forcetower.uefs.feature.shared.extensions.setValueIfNew
import com.forcetower.uefs.feature.siecomp.common.SpeakerActions
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import timber.log.Timber
import javax.inject.Inject

private const val TEN_SECONDS = 10_000L

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
                val minutes = duration.toMinutes()
                when (minutes) {
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