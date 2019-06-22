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
import com.forcetower.sagres.SagresNavigator
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.model.api.EverythingSnippet
import com.forcetower.uefs.core.model.api.UResponse
import com.forcetower.uefs.core.model.service.EvaluationDiscipline
import com.forcetower.uefs.core.model.service.EvaluationHomeTopic
import com.forcetower.uefs.core.model.service.EvaluationTeacher
import com.forcetower.uefs.core.model.service.UNESUpdate
import com.forcetower.uefs.core.model.service.discipline.DisciplineDetailsData
import com.forcetower.uefs.core.model.siecomp.ServerSession
import com.forcetower.uefs.core.model.siecomp.Speaker
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.model.unes.AccessToken
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.model.unes.Question
import com.forcetower.uefs.core.storage.network.adapter.ApiResponse
import com.forcetower.uefs.easter.darktheme.DarkInvite
import com.forcetower.uefs.easter.darktheme.DarkUnlock
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Locale

interface UService {
    @POST("oauth/token")
    @FormUrlEncoded
    fun loginWithSagres(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("grant_type") grant: String = "sagres",
        @Field("client_id") client: String = Constants.SERVICE_CLIENT_ID,
        @Field("client_secret") secret: String = Constants.SERVICE_CLIENT_SECRET,
        @Field("institution") institution: String = SagresNavigator.instance.getSelectedInstitution().toLowerCase(Locale.ROOT)
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

    @POST("account/update_fcm")
    fun sendToken(@Body data: Map<String, String>): Call<UResponse<Void>>

    @GET("courses")
    fun getCourses(): Call<List<Course>>

    @GET("synchronization")
    fun getUpdate(): Call<UNESUpdate>

    @POST("grades")
    fun sendGrades(@Body grades: DisciplineDetailsData): Call<UResponse<Void>>

    @GET("account")
    fun getAccount(): Call<Account>

    @POST("account/darktheme")
    fun requestDarkThemeUnlock(@Body invites: DarkUnlock): Call<UResponse<Void>>

    @POST("account/darktheme/invite")
    fun requestDarkSendTo(@Body invite: DarkInvite): Call<UResponse<Void>>

    // -------- Evaluation ---------
    @GET("evaluation/hot")
    fun getEvaluationTopics(): Call<List<EvaluationHomeTopic>>

    @GET("evaluation/discipline")
    fun getEvaluationDiscipline(@Query("department") department: String, @Query("code") code: String): Call<EvaluationDiscipline>

    @GET("evaluation/teacher")
    fun getTeacherById(@Query("id") teacherId: Long): Call<EvaluationTeacher>

    @GET("evaluation/question/teacher")
    fun getQuestionsForTeachers(@Query("teacher_id") teacherId: Long): Call<List<Question>>

    @GET("evaluation/question/discipline")
    fun getQuestionsForDisciplines(@Query("code") code: String, @Query("department") department: String): Call<List<Question>>

    @POST("evaluation/question/answer")
    fun answerQuestion(@Body data: MutableMap<String, Any?>): Call<UResponse<Void>>

    @GET("evaluation/everythingship")
    fun getEvaluationSnippetData(): Call<EverythingSnippet>

    // ---------------------------------------------------------------------------------------------

    @GET("siecomp/list_sessions")
    fun siecompSessions(): LiveData<ApiResponse<List<ServerSession>>>

    @POST("siecomp/speaker")
    fun createSpeaker(@Body speaker: Speaker): Call<Void>

    @POST("siecomp/edit_speaker")
    fun updateSpeaker(@Body speaker: Speaker): Call<Void>
}