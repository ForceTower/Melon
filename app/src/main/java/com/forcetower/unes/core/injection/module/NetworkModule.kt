package com.forcetower.unes.core.injection.module

import android.content.Context
import com.forcetower.unes.core.Constants
import com.forcetower.unes.core.storage.network.UService
import com.forcetower.unes.core.storage.network.adapter.LiveDataCallAdapterFactory
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
        chain.proceed(request)
    }

    @Provides
    @Singleton
    @JvmStatic
    fun provideService(client: OkHttpClient): UService {
        return Retrofit.Builder()
                .baseUrl(Constants.UNES_SERVICE_URL)
                .client(client)
                .addCallAdapterFactory(LiveDataCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(UService::class.java)
    }
}