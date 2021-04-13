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

package com.forcetower.uefs.feature.feedback

import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.R
import com.forcetower.uefs.core.storage.repository.FeedbackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val repository: FeedbackRepository,
    private val context: Context
) : ViewModel() {
    private val _sendFeedback = MediatorLiveData<Event<Boolean>>()
    val sendFeedback: LiveData<Event<Boolean>>
        get() = _sendFeedback

    private val _textError = MutableLiveData<Event<String?>>()
    val textError: LiveData<Event<String?>>
        get() = _textError

    @MainThread
    fun onSendFeedback(text: String?) {
        if (text.isNullOrBlank()) {
            _textError.value = Event(context.getString(R.string.feedback_text_empty))
        } else {
            _textError.value = Event(null)
            val source = repository.onSendFeedback(text)
            _sendFeedback.addSource(source) {
                _sendFeedback.value = Event(it)
                _sendFeedback.removeSource(source)
            }
        }
    }
}
