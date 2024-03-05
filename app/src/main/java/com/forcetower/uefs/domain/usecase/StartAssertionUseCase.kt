package com.forcetower.uefs.domain.usecase

import com.forcetower.uefs.core.model.edge.AssertionData
import com.forcetower.uefs.core.model.edge.PasskeyAssert
import com.forcetower.uefs.core.storage.repository.cloud.EdgeAuthRepository
import com.google.gson.Gson
import dagger.Reusable
import javax.inject.Inject

@Reusable
class StartAssertionUseCase @Inject constructor(
    private val auth: EdgeAuthRepository,
    private val gson: Gson
) {
    suspend operator fun invoke(): AssertionData {
        val data = auth.startAssertion()
        val parsed = gson.fromJson(data.challenge, PasskeyAssert::class.java)
        return data.copy(challenge = gson.toJson(parsed.publicKey))
    }
}