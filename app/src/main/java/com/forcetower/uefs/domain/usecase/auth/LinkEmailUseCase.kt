package com.forcetower.uefs.domain.usecase.auth

import com.forcetower.uefs.core.model.ui.edge.EmailLinkComplete
import com.forcetower.uefs.core.model.ui.edge.EmailLinkStart
import com.forcetower.uefs.core.storage.repository.cloud.EdgeAuthRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class LinkEmailUseCase @Inject constructor(
    private val repository: EdgeAuthRepository
) {
    suspend fun start(email: String): EmailLinkStart {
        return repository.emailLinkStart(email)
    }

    suspend fun finish(code: String, securityToken: String): EmailLinkComplete {
        return repository.emailLinkFinish(code, securityToken)
    }
}
