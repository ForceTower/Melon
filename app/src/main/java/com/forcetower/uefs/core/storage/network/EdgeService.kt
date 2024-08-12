package com.forcetower.uefs.core.storage.network

import com.forcetower.uefs.core.model.edge.AssertionData
import com.forcetower.uefs.core.model.edge.CompleteAssertionData
import com.forcetower.uefs.core.model.edge.EdgeAccessTokenDTO
import com.forcetower.uefs.core.model.edge.EdgeLoginBody
import com.forcetower.uefs.core.model.edge.RegisterPasskeyCredential
import com.forcetower.uefs.core.model.edge.RegisterPasskeyStart
import com.forcetower.uefs.core.model.unes.AccessToken
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface EdgeService {
    @POST("auth/login/anonymous")
    suspend fun loginAnonymous(@Body data: EdgeLoginBody): EdgeAccessTokenDTO

    @GET("auth/login/passkey/assertion/start")
    suspend fun startAssertion(): AssertionData

    @POST("auth/login/passkey/assertion/finish")
    suspend fun completeAssertion(@Body data: CompleteAssertionData): AccessToken

    @GET("passkeys/register/start")
    suspend fun registerPasskeyStart(): RegisterPasskeyStart

    @POST("passkeys/register/finish")
    suspend fun registerPasskeyFinish(@Body data: RegisterPasskeyCredential)
}
