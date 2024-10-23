package com.forcetower.uefs.feature.unesaccount.login

import androidx.lifecycle.viewModelScope
import com.forcetower.core.lifecycle.viewmodel.BaseViewModel
import com.forcetower.uefs.domain.usecase.auth.CompleteAssertionUseCase
import com.forcetower.uefs.domain.usecase.auth.EdgeAnonymousLoginUseCase
import com.forcetower.uefs.domain.usecase.auth.StartAssertionUseCase
import com.forcetower.uefs.feature.unesaccount.login.vm.LoginAccountEvent
import com.forcetower.uefs.feature.unesaccount.login.vm.LoginAccountState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class LoginAccountViewModel @Inject constructor(
    private val loginAnonymousLoginUseCase: EdgeAnonymousLoginUseCase,
    private val startAssertionUseCase: StartAssertionUseCase,
    private val completePasskeyAssertionUseCase: CompleteAssertionUseCase
) : BaseViewModel<LoginAccountState, LoginAccountEvent>(LoginAccountState()) {
    fun anonymousLogin() {
        if (currentState.loading) return
        setState { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching {
                loginAnonymousLoginUseCase.loginOrThrow()
            }.onFailure {
                Timber.e(it, "Failed to anonymously login")
                sendEvent { LoginAccountEvent.LoginFailed }
            }.onSuccess { user ->
                if (user?.email != null) {
                    sendEvent { LoginAccountEvent.SuccessHasEmail }
                } else {
                    sendEvent { LoginAccountEvent.SuccessLinkEmail }
                }
            }
            setState { it.copy(loading = false) }
        }
    }

    fun startPasskeyAssertion() {
        if (currentState.loading) return
        setState { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching {
                val result = startAssertionUseCase()
                sendEvent { LoginAccountEvent.StartPasskeyAssertion(result.flowId, result.challenge) }
            }.onFailure {
                Timber.e(it, "Failed to start assertion")
                sendEvent { LoginAccountEvent.LoginFailed }
            }
        }
    }

    fun completePasskeyAssertion(flowId: String, json: String) {
        viewModelScope.launch {
            runCatching {
                completePasskeyAssertionUseCase(flowId, json)
            }.onFailure {
                Timber.e(it, "Failed to complete assertion")
                sendEvent { LoginAccountEvent.LoginFailed }
            }.onSuccess { user ->
                if (user?.email != null) {
                    sendEvent { LoginAccountEvent.SuccessHasEmail }
                } else {
                    sendEvent { LoginAccountEvent.SuccessLinkEmail }
                }
            }
            setState { it.copy(loading = false) }
        }
    }

    fun completePasskeyLoading() {
        setState { it.copy(loading = false) }
    }
}
