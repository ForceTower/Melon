package com.forcetower.uefs.core.vm

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.storage.repository.UserSessionRepository

class UserSessionViewModel @ViewModelInject constructor(
    private val sessionRepository: UserSessionRepository
) : ViewModel() {
    fun onSessionStarted() {
        sessionRepository.onSessionStartedAsync()
    }

    fun onUserInteraction() {
        sessionRepository.onUserInteractionAsync()
    }
}