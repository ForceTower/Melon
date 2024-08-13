package com.forcetower.uefs.core.storage.repository.cloud

import com.forcetower.uefs.core.model.edge.AssertionData
import com.forcetower.uefs.core.model.edge.CompleteAssertionData
import com.forcetower.uefs.core.model.edge.EdgeLoginBody
import com.forcetower.uefs.core.model.edge.EmailLinkBodyDTO
import com.forcetower.uefs.core.model.edge.EmailLinkConfirmDTO
import com.forcetower.uefs.core.model.edge.RegisterPasskeyCredential
import com.forcetower.uefs.core.model.edge.RegisterPasskeyStart
import com.forcetower.uefs.core.model.ui.edge.EmailLinkComplete
import com.forcetower.uefs.core.model.ui.edge.EmailLinkStart
import com.forcetower.uefs.core.model.unes.EdgeAccessToken
import com.forcetower.uefs.core.model.unes.EdgeServiceAccount
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

    suspend fun completeAssertion(flowId: String, response: String): EdgeServiceAccount? {
        val token = service.completeAssertion(CompleteAssertionData(flowId, response))
        database.edgeAccessToken.insert(EdgeAccessToken(token.accessToken))

        runCatching {
            val me = service.me().data
            val value = EdgeServiceAccount(
                id = me.id,
                name = me.name,
                email = me.email,
                imageUrl = me.imageUrl,
                me = true
            )
            database.edgeServiceAccount.insertOrUpdate(value)
        }

        return database.edgeServiceAccount.requireMe()
    }

    suspend fun passkeyRegisterStart(): RegisterPasskeyStart {
        return service.registerPasskeyStart()
    }

    suspend fun passkeyRegisterFinish(flowId: String, credential: String) {
        return service.registerPasskeyFinish(RegisterPasskeyCredential(flowId, credential))
    }

    suspend fun emailLinkStart(email: String): EmailLinkStart {
        try {
            val response = service.linkEmailStart(EmailLinkBodyDTO(email))
            val body = response.body()
            if (body != null && response.isSuccessful)
                return EmailLinkStart.CodeSent(body.data.securityToken)

            return EmailLinkStart.InvalidInfo
        } catch (error: Throwable) {
            return EmailLinkStart.ConnectionError
        }
    }

    suspend fun emailLinkFinish(code: String, securityToken: String): EmailLinkComplete {
        try {
            val response = service.linkEmailComplete(EmailLinkConfirmDTO(code = code, securityToken = securityToken))
            if (response.isSuccessful) return EmailLinkComplete.Linked
            if (response.code() == 400) return EmailLinkComplete.InvalidCode
            if (response.code() == 409) return EmailLinkComplete.EmailTaken
            if (response.code() == 429) return EmailLinkComplete.TooManyTries

            return EmailLinkComplete.ConnectionError
        } catch (error: Throwable) {
            Timber.e(error, "Failed to finish registration!")
            return EmailLinkComplete.ConnectionError
        }
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

    suspend fun doAnonymousLogin(): EdgeServiceAccount? {
        val access = database.accessDao().getAccessDirectSuspend() ?: throw IllegalStateException("Access is null!")

        if (!access.valid) {
            throw IllegalStateException("Access is not in a valid state")
        }

        val result = service.loginAnonymous(EdgeLoginBody(access.username, access.password))
        Timber.i("Login completed with result $result")
        database.edgeAccessToken.insert(EdgeAccessToken(result.accessToken))

        runCatching {
            val me = service.me().data
            val value = EdgeServiceAccount(
                id = me.id,
                name = me.name,
                email = me.email,
                imageUrl = me.imageUrl,
                me = true
            )
            database.edgeServiceAccount.insertOrUpdate(value)
        }

        return database.edgeServiceAccount.requireMe()
    }
}
