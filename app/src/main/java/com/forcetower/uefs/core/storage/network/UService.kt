/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.core.storage.network

import androidx.lifecycle.LiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.model.api.EverythingSnippet
import com.forcetower.uefs.core.model.api.ImgurUpload
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
import com.forcetower.uefs.core.model.api.DarkInvite
import com.forcetower.uefs.core.model.api.DarkUnlock
import com.forcetower.uefs.core.model.service.FlowchartDTO
import com.forcetower.uefs.core.model.service.UserSessionDTO
import com.forcetower.uefs.core.model.unes.Flowchart
import com.forcetower.uefs.core.model.unes.ProfileStatement
import com.forcetower.uefs.core.model.unes.SStudentDTO
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

    @POST("account/credentials")
    fun setupAccount(@Body access: Access): Call<UResponse<Void>>

    @POST("account/profile")
    fun setupProfile(@Body profile: Profile): Call<UResponse<Void>>

    @POST("account/update_fcm")
    fun sendToken(@Body data: Map<String, String>): Call<UResponse<Void>>

    @GET("account")
    fun getAccount(): Call<Account>

    @POST("account/image")
    fun updateProfileImage(@Body data: ImgurUpload): Call<UResponse<Void>>

    @POST("account/darktheme")
    fun requestDarkThemeUnlock(@Body invites: DarkUnlock): Call<UResponse<Void>>

    @POST("account/darktheme/invite")
    fun requestDarkSendTo(@Body invite: DarkInvite): Call<UResponse<Void>>

    @GET("account/statements")
    fun getStatements(@Query("profile_id") profileId: Long): Call<UResponse<List<ProfileStatement>>>

    @POST("account/save_sessions")
    fun saveSessions(@Body session: UserSessionDTO): Call<Void>

    @GET("courses")
    fun getCourses(): Call<List<Course>>

    @GET("synchronization")
    fun getUpdate(): Call<UNESUpdate>

    @POST("grades")
    fun sendGrades(@Body grades: DisciplineDetailsData): Call<UResponse<Void>>

    // -------- Evaluation ---------
    @GET("evaluation/hot")
    fun getEvaluationTopics(): Call<List<EvaluationHomeTopic>>

    @GET("evaluation/discipline")
    fun getEvaluationDiscipline(@Query("department") department: String, @Query("code") code: String): Call<EvaluationDiscipline>

    @GET("evaluation/teacher")
    fun getTeacherById(@Query("id") teacherId: Long): Call<EvaluationTeacher>

    @GET("evaluation/teacher")
    fun getTeacherByName(@Query("name") teacherName: String): Call<EvaluationTeacher>

    @GET("evaluation/question/teacher")
    fun getQuestionsForTeachers(@Query("teacher_id") teacherId: Long): Call<List<Question>>

    @GET("evaluation/question/discipline")
    fun getQuestionsForDisciplines(@Query("code") code: String, @Query("department") department: String): Call<List<Question>>

    @POST("evaluation/question/answer")
    fun answerQuestion(@Body data: MutableMap<String, Any?>): Call<UResponse<Void>>

    @GET("evaluation/everythingship")
    fun getEvaluationSnippetData(): Call<EverythingSnippet>

    // --------- Flowchart ---------

    @GET("flowchart")
    fun getFlowcharts(): Call<UResponse<List<Flowchart>>>

    @GET("flowchart")
    fun getFlowchart(@Query("course_id") course: Long): Call<UResponse<FlowchartDTO>>

    // --------- Social -------------

    @GET("student")
    fun getStudent(@Query("student_id") studentId: Long): Call<UResponse<SStudentDTO>>

    @GET("student/me")
    fun getMeStudent(): Call<UResponse<SStudentDTO>>

    // ---------------------------------

    @GET("siecomp/list_sessions")
    fun siecompSessions(): LiveData<ApiResponse<List<ServerSession>>>

    @POST("siecomp/speaker")
    fun createSpeaker(@Body speaker: Speaker): Call<Void>

    @POST("siecomp/edit_speaker")
    fun updateSpeaker(@Body speaker: Speaker): Call<Void>
}