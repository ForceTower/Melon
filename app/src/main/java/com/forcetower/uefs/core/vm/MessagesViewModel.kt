package com.forcetower.uefs.core.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.storage.repository.MessagesRepository
import javax.inject.Inject

class MessagesViewModel @Inject constructor(
    val repository: MessagesRepository
): ViewModel() {
    val messages by lazy { repository.getMessages() }

    private val _refreshing = MediatorLiveData<Boolean>()
    val refreshing: LiveData<Boolean>
        get() = _refreshing

    fun onRefresh() {
        val fetchMessages = repository.fetchMessages()
        _refreshing.value = true
        _refreshing.addSource(fetchMessages) {
            _refreshing.removeSource(fetchMessages)
            _refreshing.value = false
        }
    }
}