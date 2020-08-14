package com.forcetower.uefs.feature.forms

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.storage.repository.FormsRepository
import com.forcetower.uefs.core.vm.Event
import timber.log.Timber

class FormsViewModel @ViewModelInject constructor(
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