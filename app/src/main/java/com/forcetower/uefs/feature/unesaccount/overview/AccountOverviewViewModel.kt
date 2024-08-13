package com.forcetower.uefs.feature.unesaccount.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.forcetower.uefs.domain.usecase.account.GetEdgeServiceAccountUseCase
import com.forcetower.uefs.domain.usecase.profile.GetProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccountOverviewViewModel @Inject constructor(
    private val getAccount: GetEdgeServiceAccountUseCase,
    getProfile: GetProfileUseCase
) : ViewModel() {
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
}