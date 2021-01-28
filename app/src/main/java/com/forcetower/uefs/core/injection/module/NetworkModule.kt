/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.core.injection.module

import android.content.Context
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.APIService
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.network.adapter.LiveDataCallAdapterFactory
import com.forcetower.uefs.core.storage.network.github.GithubService
import com.forcetower.uefs.core.util.ObjectUtils
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.net.CookieHandler
import java.net.CookieManager
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideCookieHandler(): CookieHandler = CookieManager()

    @Provides
    @Singleton
    fun provideCookieJar(context: Context): PersistentCookieJar =
        PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context))

    @Provides
    @Singleton
    fun provideOkHttpClient(cookieJar: PersistentCookieJar, interceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
            .followRedirects(true)
            .cookieJar(cookieJar)
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .addInterceptor(interceptor)
            .addInterceptor(
                HttpLoggingInterceptor {
                    Timber.tag("ok-http").d(it)
                }.apply {
                    level = if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.HEADERS
                    else
                        HttpLoggingInterceptor.Level.NONE
                }
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideInterceptor(database: UDatabase) = Interceptor { chain ->
        val request = chain.request()
        val host = request.url.host
        if (host.contains(Constants.UNES_SERVICE_BASE_URL, ignoreCase = true)) {
            val builder = request.headers.newBuilder()
                .add("Accept", "application/json")

            val token = database.accessTokenDao().getAccessTokenDirect()
            if (token?.token != null) {
                builder.add("Authorization", token.type + " " + token.token)
            }

            val newHeaders = builder.build()
            val renewed = request.newBuilder().headers(newHeaders).build()

            chain.proceed(renewed)
        } else {
            val nRequest = request.newBuilder().addHeader("Accept", "application/json").build()
            chain.proceed(nRequest)
        }
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(ZonedDateTime::class.java, ObjectUtils.ZDT_DESERIALIZER)
            .registerTypeAdapter(ZonedDateTime::class.java, ObjectUtils.ZDT_SERIALIZER)
            .serializeNulls()
            .create()
    }

    @Provides
    @Singleton
    fun provideService(client: OkHttpClient, gson: Gson): UService {
        return Retrofit.Builder()
            .baseUrl(Constants.UNES_SERVICE_URL)
            .client(client)
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(UService::class.java)
    }

    @Provides
    @Singleton
    fun provideGithubService(client: OkHttpClient): GithubService {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GithubService::class.java)
    }

    @Provides
    @Singleton
    fun provideTemporaryService(client: OkHttpClient): APIService {
        return Retrofit.Builder()
            .baseUrl(Constants.UNES_SERVICE_UPDATE)
            .client(client)
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(APIService::class.java)
    }
}
