package com.forcetower.uefs.feature.unesaccount.login

import androidx.lifecycle.viewModelScope
import com.forcetower.uefs.domain.usecase.auth.EdgeAnonymousLoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import com.forcetower.core.lifecycle.viewmodel.EventViewModel
import com.forcetower.uefs.feature.unesaccount.login.vm.LoginAccountEvent
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginAccountViewModel @Inject constructor(
    private val loginAnonymousLoginUseCase: EdgeAnonymousLoginUseCase
) : EventViewModel<LoginAccountEvent>() {
    fun anonymousLogin() {
        viewModelScope.launch {
            runCatching {
                loginAnonymousLoginUseCase.loginOrThrow()
            }.onFailure {
                Timber.e(it, "Failed to anonymously login")
                sendEvent { LoginAccountEvent.LoginFailed }
            }.onSuccess {
                sendEvent { LoginAccountEvent.LoginSuccess }
            }
        }
    }
}