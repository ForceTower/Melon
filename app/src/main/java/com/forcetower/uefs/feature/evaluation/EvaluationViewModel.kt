package com.forcetower.uefs.feature.evaluation

import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.storage.repository.cloud.AuthRepository
import javax.inject.Inject

class EvaluationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun getToken() = authRepository.getAccessToken()
}