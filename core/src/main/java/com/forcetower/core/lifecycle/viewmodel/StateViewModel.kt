package com.forcetower.core.lifecycle.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.extensions.setValueIfNew

abstract class StateViewModel<S>(initialState: S) : ViewModel() {
    var currentState: S = initialState
        private set

    private val _state = MutableLiveData(initialState)
    val state: LiveData<S> = _state

    @MainThread
    fun setState(transform: (S) -> S) {
        currentState = transform(currentState)
        _state.setValueIfNew(currentState)
    }
}
