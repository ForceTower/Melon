/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.sagres.impl

import android.content.Context

import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.SagresDatabase
import com.forcetower.sagres.executor.SagresTaskExecutor
import com.forcetower.sagres.operation.calendar.CalendarCallback
import com.forcetower.sagres.operation.calendar.CalendarOperation
import com.forcetower.sagres.operation.login.LoginOperation
import com.forcetower.sagres.operation.login.LoginCallback
import com.forcetower.sagres.operation.messages.MessagesCallback
import com.forcetower.sagres.operation.messages.MessagesOperation
import com.forcetower.sagres.operation.person.PersonCallback
import com.forcetower.sagres.operation.person.PersonOperation
import com.forcetower.sagres.operation.semester.SemesterCallback
import com.forcetower.sagres.operation.semester.SemesterOperation
import com.forcetower.sagres.operation.start_page.StartPageCallback
import com.forcetower.sagres.operation.start_page.StartPageOperation
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor

import java.util.ArrayList
import java.util.concurrent.TimeUnit

import androidx.annotation.AnyThread
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.forcetower.sagres.operation.grades.GradesCallback
import com.forcetower.sagres.operation.grades.GradesOperation
import okhttp3.Call
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient

@RestrictTo(RestrictTo.Scope.LIBRARY)
class SagresNavigatorImpl @RestrictTo(RestrictTo.Scope.LIBRARY)
private constructor(context: Context) : SagresNavigator() {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val client: OkHttpClient
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    override val database: SagresDatabase = SagresDatabase.create(context)

    init {
        this.client = createClient(context)
    }

    private fun createClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
                .followRedirects(true)
                .cookieJar(createCookieJar(context))
                .addInterceptor(createInterceptor())
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .build()
    }

    private fun createInterceptor(): Interceptor {
        return Interceptor { chain ->
            val access = database.accessDao().accessDirect
            var oRequest = chain.request()
            oRequest = oRequest.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36")
                    .build()

            if (access == null) {
                chain.proceed(oRequest)
            } else {
                val credentials = Credentials.basic(access.username, access.password)
                if (oRequest.header("Authorization") != null) {
                    chain.proceed(oRequest)
                } else {
                    val nRequest = oRequest.newBuilder()
                            .addHeader("Authorization", credentials)
                            .addHeader("Accept", "application/json")
                            .build()

                    chain.proceed(nRequest)
                }
            }
        }
    }

    private fun createCookieJar(context: Context): PersistentCookieJar {
        return PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context))
    }

    override fun stopTags(tag: String) {
        val callList = ArrayList<Call>()
        callList.addAll(client.dispatcher().runningCalls())
        callList.addAll(client.dispatcher().queuedCalls())
        for (call in callList) {
            val local = call.request().tag()
            if (local != null && local == tag) {
                call.cancel()
            }
        }
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aLogin(username: String, password: String): LiveData<LoginCallback> {
        return LoginOperation(username, password, SagresTaskExecutor.getNetworkThreadExecutor()).result
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun login(username: String, password: String): SagresNavigator? {
        val successful = LoginOperation(username, password, null).isSuccessful
        return if (successful) this else null
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aMe(): LiveData<PersonCallback> {
        return PersonOperation(null, SagresTaskExecutor.getNetworkThreadExecutor()).result
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun me(): SagresNavigator? {
        val successful = PersonOperation(null, null).isSuccess
        return if (successful) this else null
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aMessages(userId: Long): LiveData<MessagesCallback> {
        return MessagesOperation(SagresTaskExecutor.getNetworkThreadExecutor(), userId).result
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aCalendar(): LiveData<CalendarCallback> {
        return CalendarOperation(SagresTaskExecutor.getNetworkThreadExecutor()).result
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aSemesters(userId: Long): LiveData<SemesterCallback> {
        return SemesterOperation(userId, SagresTaskExecutor.getNetworkThreadExecutor()).result
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun startPage(): LiveData<StartPageCallback> {
        return StartPageOperation(SagresTaskExecutor.getNetworkThreadExecutor()).result
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun getCurrentGrades(): LiveData<GradesCallback> {
        return GradesOperation(null, null, SagresTaskExecutor.getNetworkThreadExecutor()).result
    }

    companion object {
        private var sDefaultInstance: SagresNavigatorImpl? = null
        private val sLock = Any()

        val instance: SagresNavigatorImpl
            @RestrictTo(RestrictTo.Scope.LIBRARY)
            get() = synchronized(sLock) {
                if (sDefaultInstance != null)
                    return sDefaultInstance!!
                else
                    throw IllegalStateException()
            }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun initialize(context: Context) {
            synchronized(sLock) {
                if (sDefaultInstance == null) {
                    sDefaultInstance = SagresNavigatorImpl(context.applicationContext)
                }
            }
        }
    }
}
