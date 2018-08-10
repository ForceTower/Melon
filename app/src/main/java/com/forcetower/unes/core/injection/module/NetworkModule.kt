package com.forcetower.unes.core.injection.module

import android.content.Context
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.net.CookieHandler
import java.net.CookieManager
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
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
    fun provideInterceptor(): Interceptor = Interceptor {
        val request = it.request()
        it.proceed(request)
    }
}