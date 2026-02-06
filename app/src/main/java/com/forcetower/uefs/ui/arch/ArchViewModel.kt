package com.forcetower.uefs.ui.arch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class ArchViewModel<S : Any, E : Any, I : Any>(initialState: S) : ViewModel() {
    private val _event = Channel<E>()
    val event: Flow<E> = _event.receiveAsFlow()

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state

    fun onIntent(intent: I) {
        viewModelScope.launch {
            handleIntent(intent)
        }
    }

    protected abstract suspend fun handleIntent(intent: I)

    fun syncSendEvent(producer: () -> E) {
        val action = producer()
        _event.trySend(action)
    }

    suspend fun sendEvent(producer: () -> E) {
        val action = producer()
        _event.send(action)
    }

    fun setState(transform: (S) -> S) {
        _state.update(transform)
    }

    override fun onCleared() {
        super.onCleared()
        _event.close(Exception("ViewModel was cleared"))
    }
}