package com.forcetower.uefs.domain.usecase

import com.forcetower.uefs.core.storage.repository.cloud.EdgeAuthRepository
import dagger.Reusable
import timber.log.Timber
import javax.inject.Inject

@Reusable
class CompleteAssertionUseCase @Inject constructor(
    private val auth: EdgeAuthRepository
) {
    suspend operator fun invoke(flowId: String, response: String) {
        Timber.d("Credential: $response")
        auth.completeAssertion(flowId, response)
    }
}