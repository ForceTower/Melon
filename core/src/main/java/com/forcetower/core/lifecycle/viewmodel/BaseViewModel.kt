package com.forcetower.core.lifecycle.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.extensions.setValueIfNew
import com.forcetower.core.lifecycle.LiveDataEvent

abstract class BaseViewModel<S, E>(initialState: S) : ViewModel() {
    var currentState: S = initialState
        private set

    private val _event = LiveDataEvent<E>()
    val event: LiveData<E> = _event

    private val _state = MutableLiveData(initialState)
    val state: LiveData<S> = _state

    @MainThread
    fun sendEvent(producer: () -> E) {
        _event.value = producer()
    }

    @MainThread
    fun setState(transform: (S) -> S) {
        currentState = transform(currentState)
        _state.setValueIfNew(currentState)
    }
}
