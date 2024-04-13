package com.forcetower.uefs.core.storage.repository.cloud

import com.forcetower.uefs.core.model.edge.AssertionData
import com.forcetower.uefs.core.model.edge.CompleteAssertionData
import com.forcetower.uefs.core.model.edge.RegisterPasskeyCredential
import com.forcetower.uefs.core.model.edge.RegisterPasskeyStart
import com.forcetower.uefs.core.storage.network.EdgeService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EdgeAuthRepository @Inject constructor(
    private val service: EdgeService
) {
    suspend fun startAssertion(): AssertionData {
        return service.startAssertion()
    }

    suspend fun completeAssertion(flowId: String, response: String) {
        val token = service.completeAssertion(CompleteAssertionData(flowId, response))
        Timber.d("Token $token")
    }

    suspend fun registerStart(): RegisterPasskeyStart {
        return service.registerPasskeyStart()
    }

    suspend fun registerFinish(flowId: String, credential: String) {
        return service.registerPasskeyFinish(RegisterPasskeyCredential(flowId, credential))
    }
}
