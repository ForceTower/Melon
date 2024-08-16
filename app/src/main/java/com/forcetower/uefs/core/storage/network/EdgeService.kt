package com.forcetower.uefs.core.storage.network

import com.forcetower.uefs.core.model.edge.auth.AssertionData
import com.forcetower.uefs.core.model.edge.account.ChangePictureDTO
import com.forcetower.uefs.core.model.edge.auth.CompleteAssertionData
import com.forcetower.uefs.core.model.edge.auth.EdgeAccessTokenDTO
import com.forcetower.uefs.core.model.edge.auth.EdgeLoginBody
import com.forcetower.uefs.core.model.edge.auth.EmailLinkBodyDTO
import com.forcetower.uefs.core.model.edge.auth.EmailLinkConfirmDTO
import com.forcetower.uefs.core.model.edge.auth.LinkEmailResponseDTO
import com.forcetower.uefs.core.model.edge.auth.RegisterPasskeyCredential
import com.forcetower.uefs.core.model.edge.auth.RegisterPasskeyStart
import com.forcetower.uefs.core.model.edge.account.SendMessagingTokenDTO
import com.forcetower.uefs.core.model.edge.account.ServiceAccountDTO
import com.forcetower.uefs.core.model.edge.ServiceResponseWrapper
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface EdgeService {
    @POST("auth/login/anonymous")
    suspend fun loginAnonymous(@Body data: EdgeLoginBody): EdgeAccessTokenDTO

    @GET("auth/login/passkey/assertion/start")
    suspend fun startAssertion(): AssertionData

    @POST("auth/login/passkey/assertion/finish")
    suspend fun completeAssertion(@Body data: CompleteAssertionData): EdgeAccessTokenDTO

    @GET("passkeys/register/start")
    suspend fun registerPasskeyStart(): RegisterPasskeyStart

    @POST("passkeys/register/finish")
    suspend fun registerPasskeyFinish(@Body data: RegisterPasskeyCredential)

    @GET("account/me")
    suspend fun me(): ServiceResponseWrapper<ServiceAccountDTO>

    @POST("account/register/start")
    suspend fun linkEmailStart(@Body data: EmailLinkBodyDTO): Response<ServiceResponseWrapper<LinkEmailResponseDTO>>

    @POST("account/register/complete")
    suspend fun linkEmailComplete(@Body data: EmailLinkConfirmDTO): Response<ServiceResponseWrapper<String>>

    @POST("account/fcm")
    suspend fun fcm(@Body data: SendMessagingTokenDTO): ServiceResponseWrapper<String>

    @POST("account/picture")
    suspend fun uploadPicture(@Body data: ChangePictureDTO): ServiceResponseWrapper<String>
}
