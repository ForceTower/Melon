package com.forcetower.uefs.domain.usecase.auth

import com.forcetower.uefs.core.model.edge.auth.AssertionData
import com.forcetower.uefs.core.model.edge.auth.PasskeyAssert
import com.forcetower.uefs.core.storage.repository.cloud.EdgeAuthRepository
import com.forcetower.uefs.domain.model.auth.AssertionDataUI
import com.google.gson.Gson
import dagger.Reusable
import javax.inject.Inject

@Reusable
class StartAssertionUseCase @Inject constructor(
    private val auth: EdgeAuthRepository,
    private val gson: Gson
) {
    suspend operator fun invoke(): AssertionDataUI {
        val data = auth.startAssertion()
        return AssertionDataUI(
            flowId = data.flowId,
            challenge = gson.toJson(data.challenge.publicKey)
        )
    }
}
