package com.forcetower.uefs.core.storage.network

import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.model.cloud.AccessToken
import com.forcetower.uefs.core.model.cloud.helpers.AWResponse
import com.forcetower.uefs.core.model.unes.Access
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AWService {
    @POST("oauth/token")
    @FormUrlEncoded
    fun loginWithSagres(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("grant_type") grand: String = "sagres",
        @Field("client_id") client: String = Constants.SERVICE_CLIENT_ID,
        @Field("client_secret") secret: String = Constants.SERVICE_CLIENT_SECRET
    ): Call<AccessToken>

    @POST("oauth/token")
    @FormUrlEncoded
    fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("grant_type") grand: String = "password",
        @Field("client_id") client: String = Constants.SERVICE_CLIENT_ID,
        @Field("client_secret") secret: String = Constants.SERVICE_CLIENT_SECRET
    ): Call<AccessToken>

    @POST("account")
    fun setupAccount(access: Access): Call<AWResponse<Void>>
}