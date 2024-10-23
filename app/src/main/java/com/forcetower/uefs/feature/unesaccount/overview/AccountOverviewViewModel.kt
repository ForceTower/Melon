package com.forcetower.uefs.feature.unesaccount.overview

import android.net.Uri
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.forcetower.core.lifecycle.viewmodel.BaseViewModel
import com.forcetower.uefs.domain.usecase.account.ChangeProfilePictureUseCase
import com.forcetower.uefs.domain.usecase.account.GetEdgeServiceAccountUseCase
import com.forcetower.uefs.domain.usecase.auth.RegisterPasskeyUseCase
import com.forcetower.uefs.domain.usecase.profile.GetProfileUseCase
import com.forcetower.uefs.feature.unesaccount.overview.vm.AccountOverviewEvent
import com.forcetower.uefs.feature.unesaccount.overview.vm.AccountOverviewState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class AccountOverviewViewModel @Inject constructor(
    private val getAccount: GetEdgeServiceAccountUseCase,
    private val registerPasskeyUseCase: RegisterPasskeyUseCase,
    getProfile: GetProfileUseCase,
    private val changeProfilePictureUseCase: ChangeProfilePictureUseCase
) : BaseViewModel<AccountOverviewState, AccountOverviewEvent>(
    AccountOverviewState()
) {
    val user = getAccount().asLiveData()
    val profile = getProfile().asLiveData()

    fun fetch() {
        viewModelScope.launch {
            runCatching {
                getAccount.update()
            }.onFailure {
                Timber.w(it, "Failed to update user")
            }
        }
    }

    fun registerPasskeyStart() {
        if (currentState.loading) return
        setState { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching {
                val start = registerPasskeyUseCase.start()
                sendEvent { AccountOverviewEvent.PasskeyRegister(start.flowId, start.create) }
            }.onFailure {
                Timber.e(it, "Failed to start registration")
                sendEvent { AccountOverviewEvent.PasskeyRegisterConnectionFailed }
            }
        }
    }

    fun registerPasskeyFinish(flowId: String, json: String) {
        viewModelScope.launch {
            runCatching {
                registerPasskeyUseCase.finish(flowId, json)
            }.onFailure {
                Timber.e(it, "Failed to finish registration")
                sendEvent { AccountOverviewEvent.PasskeyRegisterConnectionFailed }
            }.onSuccess {
                sendEvent { AccountOverviewEvent.PasskeyRegisterCompleted }
            }

            onPasskeyRegistrationFinished()
        }
    }

    fun onPasskeyRegistrationFinished() {
        setState { it.copy(loading = false) }
    }

    fun uploadProfilePicture(uri: Uri) {
        setState { it.copy(uploadingPicture = true) }
        viewModelScope.launch {
            runCatching {
                changeProfilePictureUseCase(uri)
            }.onFailure {
                Timber.e(it, "Failed to update image")
                sendEvent { AccountOverviewEvent.ImageUpdateFailed }
            }
        }

        setState { it.copy(uploadingPicture = false) }
    }
}
