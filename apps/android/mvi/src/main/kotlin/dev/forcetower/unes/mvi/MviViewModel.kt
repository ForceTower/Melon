package dev.forcetower.unes.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<E>(
        capacity = Channel.BUFFERED,
        onBufferOverflow = BufferOverflow.SUSPEND,
    )
    val effects: Flow<E> = _effects.receiveAsFlow()

    protected val currentState: S
        get() = _state.value

    abstract fun onIntent(intent: I)

    protected fun setState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    protected fun emitEffect(effect: E) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
