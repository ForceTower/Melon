package com.forcetower.uefs.feature.unesaccount.confirm

import androidx.lifecycle.viewModelScope
import com.forcetower.core.lifecycle.viewmodel.BaseViewModel
import com.forcetower.uefs.core.model.ui.edge.EmailLinkComplete
import com.forcetower.uefs.domain.usecase.auth.LinkEmailUseCase
import com.forcetower.uefs.feature.unesaccount.confirm.vm.ConfirmEmailAccountEvent
import com.forcetower.uefs.feature.unesaccount.confirm.vm.ConfirmEmailAccountState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ConfirmEmailAccountViewModel @Inject constructor(
    private val linkEmail: LinkEmailUseCase
) : BaseViewModel<ConfirmEmailAccountState, ConfirmEmailAccountEvent>(ConfirmEmailAccountState()) {
    fun submit(code: String, securityToken: String) {
        if (currentState.loading) return
        if (code.isBlank()) return

        setState { it.copy(loading = true) }

        viewModelScope.launch {
            val result = linkEmail.finish(code, securityToken)
            val event = when (result) {
                EmailLinkComplete.ConnectionError -> ConfirmEmailAccountEvent.ConnectionFailed
                EmailLinkComplete.EmailTaken -> ConfirmEmailAccountEvent.EmailTaken
                EmailLinkComplete.InvalidCode -> ConfirmEmailAccountEvent.InvalidCode
                EmailLinkComplete.Linked -> ConfirmEmailAccountEvent.Completed
                EmailLinkComplete.TooManyTries -> ConfirmEmailAccountEvent.TooManyTries
            }

            sendEvent { event }
            setState { it.copy(loading = false) }
        }
    }
}
