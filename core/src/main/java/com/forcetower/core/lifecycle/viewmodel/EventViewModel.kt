package com.forcetower.core.lifecycle.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.SingleLiveEvent

abstract class EventViewModel<E> : ViewModel() {
    private val _event = SingleLiveEvent<E>()
    val event: LiveData<E> = _event

    @MainThread
    fun sendEvent(producer: () -> E) {
        _event.value = producer()
    }
}
