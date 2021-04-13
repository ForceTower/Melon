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

package com.forcetower.uefs.feature.forms

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.core.storage.repository.FormsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FormsViewModel @Inject constructor(
    private val repository: FormsRepository
) : ViewModel() {
    private val answers = mutableMapOf<String, String>()

    private val _nextQuestion = MutableLiveData<Event<Unit>>()
    val nextQuestion: LiveData<Event<Unit>>
        get() = _nextQuestion

    val account = repository.account

    fun onNextQuestion() {
        _nextQuestion.value = Event(Unit)
    }

    fun answer(id: String, rating: Float) {
        answers[id] = rating.toInt().toString()
    }

    fun answer(id: String, value: String) {
        answers[id] = value
    }

    fun submitAnswers() {
        Timber.d("Submitting answers... $answers")
        repository.submitAnswers(answers)
    }
}
