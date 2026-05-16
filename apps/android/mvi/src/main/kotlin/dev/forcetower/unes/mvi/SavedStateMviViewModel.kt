package dev.forcetower.unes.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

abstract class SavedStateMviViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    protected val savedStateHandle: SavedStateHandle,
    initialState: S,
) : MviViewModel<S, I, E>(initialState) {

    protected fun <T> persistStateSlice(key: String, selector: (S) -> T) {
        viewModelScope.launch {
            state
                .map(selector)
                .distinctUntilChanged()
                .drop(1)
                .collect { savedStateHandle[key] = it }
        }
    }
}
