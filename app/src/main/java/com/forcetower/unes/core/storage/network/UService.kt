package com.forcetower.unes.core.storage.network

import com.forcetower.unes.core.model.AccessToken
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface UService {
    @POST("/login")
    @FormUrlEncoded
    fun login(@Field("username") username: String, @Field("password") password: String): Call<AccessToken>

    @POST("/login_create")
    @FormUrlEncoded
    fun loginOrCreate(
            @Field("username") username: String,
            @Field("password") password: String,
            @Field("name") name: String,
            @Field("email") email: String,
            @Field("cpf") cpf: String,
            @Field("appToken") token: String
    ): Call<AccessToken>
}