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
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.storage.cookies.CachedCookiePersistor
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.EdgeService
import com.forcetower.uefs.core.storage.network.ParadoxService
import com.forcetower.uefs.core.storage.network.UService
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
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
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
import javax.inject.Named
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
    fun provideCachedCookiePersist(context: Context): CachedCookiePersistor {
        return CachedCookiePersistor(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: PersistentCookieJar,
        interceptor: Interceptor,
        chuckerInterceptor: ChuckerInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT)) // Needed for old sagres, omegalul
            .followRedirects(true)
            .cookieJar(cookieJar)
            .callTimeout(2, TimeUnit.MINUTES)
            .addInterceptor(interceptor)
            .addInterceptor(
                HttpLoggingInterceptor {
                    Timber.tag("ok-http").d(it)
                }.apply {
                    level = if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.BASIC
                    else
                        HttpLoggingInterceptor.Level.NONE
                }
            )
            .addInterceptor(chuckerInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideInterceptor(
        database: UDatabase,
        @Named("webViewUA") userAgent: String
    ) = Interceptor { chain ->
        val request = chain.request()
        val host = request.url.host
        // Normal contains edge
        if (host.contains(Constants.EDGE_UNES_SERVICE_BASE_URL, ignoreCase = true)) {
            val builder = request.headers.newBuilder()
                .add("Accept", "application/json")
                .set("User-Agent", userAgent)

            val token = runBlocking { database.edgeAccessToken.require() }
            if (token?.accessToken != null) {
                builder.add("Authorization", "Bearer ${token.accessToken}")
            }

            val newHeaders = builder.build()
            val renewed = request.newBuilder().headers(newHeaders).build()

            chain.proceed(renewed)
        } else if (host.contains(Constants.UNES_SERVICE_BASE_URL, ignoreCase = true)) {
            val builder = request.headers.newBuilder()
                .add("Accept", "application/json")
                .set("User-Agent", userAgent)

            val token = database.accessTokenDao().getAccessTokenDirect()
            if (token?.token != null) {
                builder.add("Authorization", token.type + " " + token.token)
            }

            val newHeaders = builder.build()
            val renewed = request.newBuilder().headers(newHeaders).build()

            chain.proceed(renewed)
        }  else {
            val nRequest = request.newBuilder().addHeader("Accept", "application/json").build()
            chain.proceed(nRequest)
        }
    }

    @Provides
    @Singleton
    fun chuckerInterceptor(
        @ApplicationContext context: Context
    ): ChuckerInterceptor {
        val chuckerCollector = ChuckerCollector(
            context = context,
            showNotification = true,
            retentionPeriod = RetentionManager.Period.ONE_WEEK
        )

        return ChuckerInterceptor.Builder(context)
            .collector(chuckerCollector)
            .alwaysReadResponseBody(true)
            .createShortcut(true)
            .build()
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
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GithubService::class.java)
    }

    @Provides
    @Singleton
    fun provideEdgeService(client: OkHttpClient): EdgeService {
        return Retrofit.Builder()
            .baseUrl(Constants.EDGE_UNES_SERVICE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EdgeService::class.java)
    }

    @Provides
    @Singleton
    fun provideParadoxService(client: OkHttpClient): ParadoxService {
        return Retrofit.Builder()
            .baseUrl(Constants.EDGE_UNES_SERVICE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ParadoxService::class.java)
    }
}
