package com.forcetower.uefs.core.vm

import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.storage.repository.UserSessionRepository
import javax.inject.Inject

class UserSessionViewModel @Inject constructor(
    private val sessionRepository: UserSessionRepository
) : ViewModel() {
    fun onSessionStarted() {
        sessionRepository.onSessionStartedAsync()
    }

    fun onUserInteraction() {
        sessionRepository.onUserInteractionAsync()
    }
}