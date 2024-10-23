package com.forcetower.uefs.domain.usecase.auth

import com.forcetower.uefs.core.model.unes.EdgeServiceAccount
import com.forcetower.uefs.core.storage.repository.cloud.EdgeAuthRepository
import dagger.Reusable
import javax.inject.Inject
import timber.log.Timber

@Reusable
class CompleteAssertionUseCase @Inject constructor(
    private val auth: EdgeAuthRepository
) {
    suspend operator fun invoke(flowId: String, response: String): EdgeServiceAccount? {
        Timber.d("Credential: $response")
        return auth.completeAssertion(flowId, response)
    }
}
