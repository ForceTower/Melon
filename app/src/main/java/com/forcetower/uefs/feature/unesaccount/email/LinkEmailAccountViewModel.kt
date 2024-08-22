package com.forcetower.uefs.feature.unesaccount.email

import android.util.Patterns
import androidx.lifecycle.viewModelScope
import com.forcetower.core.lifecycle.viewmodel.BaseViewModel
import com.forcetower.uefs.core.model.ui.edge.EmailLinkStart
import com.forcetower.uefs.domain.usecase.auth.LinkEmailUseCase
import com.forcetower.uefs.feature.unesaccount.email.vm.LinkEmailAccountEvent
import com.forcetower.uefs.feature.unesaccount.email.vm.LinkEmailAccountState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LinkEmailAccountViewModel @Inject constructor(
    private val linkEmail: LinkEmailUseCase
) : BaseViewModel<LinkEmailAccountState, LinkEmailAccountEvent>(LinkEmailAccountState()) {
    fun link(email: String) {
        if (currentState.loading) return

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            sendEvent { LinkEmailAccountEvent.InvalidEmail }
            return
        }

        setState { it.copy(loading = true) }

        viewModelScope.launch {
            val event = when (val result = linkEmail.start(email)) {
                is EmailLinkStart.CodeSent -> LinkEmailAccountEvent.EmailSent(result.securityCode, email)
                EmailLinkStart.ConnectionError -> LinkEmailAccountEvent.SendError
                EmailLinkStart.InvalidInfo -> LinkEmailAccountEvent.InvalidInfo
            }

            sendEvent { event }
            setState { it.copy(loading = false) }
        }
    }
}
