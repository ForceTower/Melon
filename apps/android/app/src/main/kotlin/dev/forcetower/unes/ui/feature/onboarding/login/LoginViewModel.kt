package dev.forcetower.unes.ui.feature.onboarding.login

import android.app.Activity
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.auth.domain.model.LoginError
import dev.forcetower.melon.feature.auth.domain.usecase.BeginPasskeyLoginUseCase
import dev.forcetower.melon.feature.auth.domain.usecase.CompletePasskeyLoginUseCase
import dev.forcetower.melon.feature.auth.domain.usecase.LoginUseCase
import dev.forcetower.unes.R
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val isLoading: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val errorArg: String? = null,
    val warnedEmailValue: String? = null,
) : UiState {
    val canSubmit: Boolean
        get() = !isLoading && username.isNotBlank() && password.isNotBlank()
}

sealed interface LoginIntent : UiIntent {
    data class UsernameChanged(val value: String) : LoginIntent
    data class PasswordChanged(val value: String) : LoginIntent
    data object TogglePasswordVisibility : LoginIntent
    data object DismissError : LoginIntent
    data object Submit : LoginIntent
    data class SubmitPasskey(val activity: Activity) : LoginIntent
}

sealed interface LoginEffect : UiEffect {
    data class Authenticated(val firstName: String) : LoginEffect
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val beginPasskeyLogin: BeginPasskeyLoginUseCase,
    private val completePasskeyLogin: CompletePasskeyLoginUseCase,
    private val passkeyClient: PasskeyClient,
) : MviViewModel<LoginUiState, LoginIntent, LoginEffect>(LoginUiState()) {

    override fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UsernameChanged -> setState { copy(username = intent.value, errorRes = null, errorArg = null) }
            is LoginIntent.PasswordChanged -> setState { copy(password = intent.value, errorRes = null, errorArg = null) }
            LoginIntent.TogglePasswordVisibility -> setState { copy(showPassword = !showPassword) }
            LoginIntent.DismissError -> setState { copy(errorRes = null, errorArg = null) }
            LoginIntent.Submit -> submit()
            is LoginIntent.SubmitPasskey -> submitPasskey(intent.activity)
        }
    }

    private fun submit() {
        val state = currentState
        if (!state.canSubmit) return
        val trimmedUsername = state.username.trim()
        if (trimmedUsername.contains('@') && state.warnedEmailValue != trimmedUsername) {
            setState {
                copy(
                    errorRes = R.string.onboarding_login_error_email_warning,
                    errorArg = null,
                    warnedEmailValue = trimmedUsername,
                )
            }
            return
        }
        setState { copy(isLoading = true, errorRes = null, errorArg = null) }
        viewModelScope.launch {
            val result = loginUseCase(trimmedUsername, state.password)
            handleLoginResult(result)
        }
    }

    private fun submitPasskey(activity: Activity) {
        if (currentState.isLoading) return
        setState { copy(isLoading = true, errorRes = null, errorArg = null) }
        viewModelScope.launch {
            when (val begin = beginPasskeyLogin(username = null)) {
                is Outcome.Err -> setState {
                    copy(isLoading = false, errorRes = begin.error.toMessageRes(), errorArg = begin.error.serverMessage())
                }
                is Outcome.Ok -> {
                    val assertion = try {
                        passkeyClient.assert(begin.value, activity)
                    } catch (e: PasskeyClient.PasskeyException) {
                        setState { copy(isLoading = false, errorRes = e.toMessageRes()) }
                        return@launch
                    }
                    val complete = completePasskeyLogin(begin.value.sessionId, assertion)
                    handleLoginResult(complete)
                }
            }
        }
    }

    private suspend fun handleLoginResult(result: Outcome<dev.forcetower.melon.core.session.domain.model.User, LoginError>) {
        when (result) {
            is Outcome.Ok -> {
                setState { copy(isLoading = false, errorRes = null, errorArg = null) }
                emitEffect(LoginEffect.Authenticated(result.value.name.firstName()))
            }
            is Outcome.Err -> setState {
                copy(isLoading = false, errorRes = result.error.toMessageRes(), errorArg = result.error.serverMessage())
            }
        }
    }
}

private fun LoginError.toMessageRes(): Int = when (this) {
    LoginError.Kind.NoConnection -> R.string.onboarding_login_error_no_connection
    LoginError.Kind.InvalidCredentials -> R.string.onboarding_login_error_invalid_credentials
    LoginError.Kind.Unexpected -> R.string.onboarding_login_error_unexpected
    is LoginError.Server -> R.string.onboarding_login_error_server
    is LoginError.TlsIntercepted ->
        if (issuerName != null) R.string.onboarding_login_error_tls_intercepted_named
        else R.string.onboarding_login_error_tls_intercepted_unnamed
    LoginError.TlsClockSkew -> R.string.onboarding_login_error_tls_clock_skew
    LoginError.TlsGeneric -> R.string.onboarding_login_error_tls_generic
}

private fun LoginError.serverMessage(): String? = when (this) {
    is LoginError.Server -> message
    is LoginError.TlsIntercepted -> issuerName
    else -> null
}

private fun PasskeyClient.PasskeyException.toMessageRes(): Int = when (this) {
    is PasskeyClient.PasskeyException.NotSupported -> R.string.onboarding_login_passkey_not_supported
    is PasskeyClient.PasskeyException.InvalidChallenge -> R.string.onboarding_login_passkey_invalid_challenge
    is PasskeyClient.PasskeyException.NoCredential -> R.string.onboarding_login_passkey_no_credential
    is PasskeyClient.PasskeyException.Cancelled -> R.string.onboarding_login_passkey_no_credential
    is PasskeyClient.PasskeyException.Unknown -> R.string.onboarding_login_error_unexpected
}

private fun String.firstName(): String = trim().split(' ', '\t', '\n').firstOrNull().orEmpty()
