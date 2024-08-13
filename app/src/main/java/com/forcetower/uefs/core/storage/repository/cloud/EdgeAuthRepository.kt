package com.forcetower.uefs.core.storage.repository.cloud

import com.forcetower.uefs.core.model.edge.AssertionData
import com.forcetower.uefs.core.model.edge.CompleteAssertionData
import com.forcetower.uefs.core.model.edge.EdgeAccessTokenDTO
import com.forcetower.uefs.core.model.edge.EdgeLoginBody
import com.forcetower.uefs.core.model.edge.RegisterPasskeyCredential
import com.forcetower.uefs.core.model.edge.RegisterPasskeyStart
import com.forcetower.uefs.core.model.unes.EdgeAccessToken
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.EdgeService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EdgeAuthRepository @Inject constructor(
    private val service: EdgeService,
    private val database: UDatabase
) {
    suspend fun anonymous(username: String, password: String) {
        val result = service.loginAnonymous(EdgeLoginBody(username, password))
        database.edgeAccessToken.insert(EdgeAccessToken(result.accessToken))
    }

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

    suspend fun prepareAndLogin() {
        val current = database.edgeAccessToken.require()
        if (current != null) {
            Timber.i("Current access already exists!")
            return
        }

        val access = database.accessDao().getAccessDirectSuspend()
        if (access == null) {
            Timber.i("Access is null! How?")
            return
        }

        if (!access.valid) {
            Timber.i("Access is not valid. Skipping!")
            return
        }

        val result = service.loginAnonymous(EdgeLoginBody(access.username, access.password))
        Timber.i("Login completed with result $result")
        database.edgeAccessToken.insert(EdgeAccessToken(result.accessToken))
    }
}
