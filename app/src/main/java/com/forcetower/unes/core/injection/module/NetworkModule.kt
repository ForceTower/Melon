/*
 * Copyright (c) 2018.
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

package com.forcetower.unes.core.injection.module

import android.content.Context
import com.forcetower.unes.core.Constants
import com.forcetower.unes.core.storage.network.UService
import com.forcetower.unes.core.storage.network.adapter.LiveDataCallAdapterFactory
import com.forcetower.unes.core.util.ObjectUtils
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.threeten.bp.ZonedDateTime
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.net.CookieHandler
import java.net.CookieManager
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
object NetworkModule {

    @Provides
    @Singleton
    @JvmStatic
    fun provideCookieHandler(): CookieHandler = CookieManager()

    @Provides
    @Singleton
    @JvmStatic
    fun provideCookieJar(context: Context): PersistentCookieJar =
            PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context))

    @Provides
    @Singleton
    @JvmStatic
    fun provideOkHttpClient(cookieJar: PersistentCookieJar, interceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder()
                .followRedirects(true)
                .cookieJar(cookieJar)
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .addInterceptor(interceptor)
                .build()
    }

    @Provides
    @Singleton
    @JvmStatic
    fun provideInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request()
        Timber.d("Going to: ${request.url().url()}")
        val nRequest = request.newBuilder().addHeader("Accept", "application/json").build()
        chain.proceed(nRequest)
    }

    @Provides
    @Singleton
    @JvmStatic
    fun provideGson(): Gson {
        return GsonBuilder()
                .registerTypeAdapter(ZonedDateTime::class.java, ObjectUtils.ZDT_DESERIALIZER)
                .create()
    }

    @Provides
    @Singleton
    @JvmStatic
    fun provideService(client: OkHttpClient, gson: Gson): UService {
        return Retrofit.Builder()
                .baseUrl(Constants.UNES_SERVICE_URL)
                .client(client)
                .addCallAdapterFactory(LiveDataCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(UService::class.java)
    }
}