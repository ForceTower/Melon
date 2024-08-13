package com.forcetower.uefs.domain.usecase.auth

import com.forcetower.uefs.core.model.unes.EdgeServiceAccount
import com.forcetower.uefs.core.storage.repository.cloud.EdgeAuthRepository
import dagger.Reusable
import timber.log.Timber
import javax.inject.Inject

@Reusable
class CompleteAssertionUseCase @Inject constructor(
    private val auth: EdgeAuthRepository
) {
    suspend operator fun invoke(flowId: String, response: String): EdgeServiceAccount? {
        Timber.d("Credential: $response")
        return auth.completeAssertion(flowId, response)
    }
}
