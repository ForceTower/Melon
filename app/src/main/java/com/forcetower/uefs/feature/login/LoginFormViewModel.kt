package com.forcetower.uefs.feature.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forcetower.uefs.core.model.edge.RegisterPasskeyStart
import com.forcetower.uefs.domain.usecase.auth.CompleteAssertionUseCase
import com.forcetower.uefs.domain.usecase.auth.RegisterPasskeyUseCase
import com.forcetower.uefs.domain.usecase.auth.StartAssertionUseCase
import com.forcetower.uefs.feature.shared.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginFormViewModel @Inject constructor(
    private val getLoginChallenge: StartAssertionUseCase,
    private val completeAssertion: CompleteAssertionUseCase,
    private val registerPasskey: RegisterPasskeyUseCase
) : ViewModel() {
    private val _data = SingleLiveEvent<String>()
    val challenge: LiveData<String> = _data

    private val _register = SingleLiveEvent<RegisterPasskeyStart>()
    val register: LiveData<RegisterPasskeyStart> = _register

    private var flowId = ""

    fun startRegister() {
        viewModelScope.launch {
            runCatching {
                val data = registerPasskey.start()
                _register.value = data
            }.onFailure {
                Timber.e(it, "Failed to request challenge")
            }
        }
    }

    fun finishRegister(flowId: String, credential: String) {
        viewModelScope.launch {
            runCatching {
                registerPasskey.finish(flowId, credential)
            }.onFailure {
                Timber.e(it, "Failed to register")
            }
        }
    }

    fun startAssertion() {
        viewModelScope.launch {
            runCatching {
                val data = getLoginChallenge()
                flowId = data.flowId
                val challenge = data.challenge
                _data.value = challenge
            }.onFailure {
                Timber.e(it, "Failed to request assertion")
            }
        }
    }

    fun completeAssertion(responseJson: String) {
        if (flowId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                completeAssertion(flowId, responseJson)
            }.onFailure {
                Timber.e(it, "Failed to authenticate user")
            }
        }
    }
}
