/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.uefs.core.storage.network

import androidx.lifecycle.LiveData
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.model.api.UResponse
import com.forcetower.uefs.core.model.service.UNESUpdate
import com.forcetower.uefs.core.model.siecomp.ServerSession
import com.forcetower.uefs.core.model.siecomp.Speaker
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.model.unes.AccessToken
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.storage.network.adapter.ApiResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface UService {
    @POST("oauth/token")
    @FormUrlEncoded
    fun loginWithSagres(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("grant_type") grant: String = "sagres",
        @Field("client_id") client: String = Constants.SERVICE_CLIENT_ID,
        @Field("client_secret") secret: String = Constants.SERVICE_CLIENT_SECRET,
        @Field("institution") institution: String = Constants.SERVICE_CLIENT_INSTITUTION
    ): Call<AccessToken>

    @POST("oauth/token")
    @FormUrlEncoded
    fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("grant_type") grant: String = "password",
        @Field("client_id") client: String = Constants.SERVICE_CLIENT_ID,
        @Field("client_secret") secret: String = Constants.SERVICE_CLIENT_SECRET
    ): Call<AccessToken>

    @GET("account")
    fun account(): Call<Profile>

    @POST("account/credentials")
    fun setupAccount(@Body access: Access): Call<UResponse<Void>>

    @POST("account/profile")
    fun setupProfile(@Body profile: Profile): Call<UResponse<Void>>

    @GET("courses")
    fun getCourses(): Call<List<Course>>

    @GET("synchronization")
    fun getUpdate(): Call<UNESUpdate>

    // ---------------------------------------------------------------------------------------------

    @GET("siecomp/list_sessions")
    fun siecompSessions(): LiveData<ApiResponse<List<ServerSession>>>

    @POST("siecomp/speaker")
    fun createSpeaker(@Body speaker: Speaker): Call<Void>

    @POST("siecomp/edit_speaker")
    fun updateSpeaker(@Body speaker: Speaker): Call<Void>
}