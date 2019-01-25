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

package com.forcetower.sagres.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.preference.PreferenceManager
import androidx.annotation.AnyThread
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import com.forcetower.sagres.Constants
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.SagresDatabase
import com.forcetower.sagres.database.model.SDemandOffer
import com.forcetower.sagres.executor.SagresTaskExecutor
import com.forcetower.sagres.operation.calendar.CalendarCallback
import com.forcetower.sagres.operation.calendar.CalendarOperation
import com.forcetower.sagres.operation.demand.CreateDemandOperation
import com.forcetower.sagres.operation.demand.DemandCreatorCallback
import com.forcetower.sagres.operation.demand.DemandOffersCallback
import com.forcetower.sagres.operation.demand.LoadDemandOffersOperation
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsOperation
import com.forcetower.sagres.operation.document.DocumentCallback
import com.forcetower.sagres.operation.document.DocumentOperation
import com.forcetower.sagres.operation.grades.GradesCallback
import com.forcetower.sagres.operation.grades.GradesOperation
import com.forcetower.sagres.operation.login.LoginCallback
import com.forcetower.sagres.operation.login.LoginOperation
import com.forcetower.sagres.operation.messages.MessagesCallback
import com.forcetower.sagres.operation.messages.MessagesOperation
import com.forcetower.sagres.operation.messages.OldMessagesOperation
import com.forcetower.sagres.operation.person.PersonCallback
import com.forcetower.sagres.operation.person.PersonOperation
import com.forcetower.sagres.operation.semester.SemesterCallback
import com.forcetower.sagres.operation.semester.SemesterOperation
import com.forcetower.sagres.operation.servicerequest.RequestedServicesCallback
import com.forcetower.sagres.operation.servicerequest.RequestedServicesOperation
import com.forcetower.sagres.operation.start_page.StartPageCallback
import com.forcetower.sagres.operation.start_page.StartPageOperation
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import okhttp3.Call
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.ArrayList
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY)
class SagresNavigatorImpl @RestrictTo(RestrictTo.Scope.LIBRARY)
private constructor(context: Context) : SagresNavigator() {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val defaultClient: OkHttpClient
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    override val database: SagresDatabase = SagresDatabase.create(context)
    private lateinit var cookieJar: PersistentCookieJar
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val proxyClient: OkHttpClient
    private val connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
    private val wifiManager = ContextCompat.getSystemService(context, WifiManager::class.java)

    val client: OkHttpClient
        get() {
            if (proxyConfigured && shouldUseProxy())
                return proxyClient
            return defaultClient
        }

    var proxyConfigured = false

    init {
        this.defaultClient = createClient(context)
        this.proxyClient = createProxyClient(context, defaultClient)
    }

    private fun createProxyClient(context: Context, parentClient: OkHttpClient): OkHttpClient {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        proxyConfigured = preferences.getBoolean("stg_sync_auto_proxy", true) // TODO Activated for testing
        val proxy = preferences.getString("stg_sync_proxy", "10.65.16.2:3128")!!
        val splits = proxy.split(":")

        var ip = "10.65.16.2"
        var gate = 3128

        if (splits.size == 2) {
            ip = splits[0]
            gate = splits[1].toIntOrNull() ?: 3128
        }

        return parentClient.newBuilder()
            .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(ip, gate)))
            .build()
    }

    private fun createClient(context: Context): OkHttpClient {
        cookieJar = createCookieJar(context)
        return OkHttpClient.Builder()
                .followRedirects(true)
                .cookieJar(cookieJar)
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

    private fun shouldUseProxy(): Boolean {
        val connectivity = connectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val capabilities = connectivity?.getNetworkCapabilities(connectivity.activeNetwork)
            if (capabilities != null) {
                val wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                if (wifi) {
                    val manager = wifiManager
                    val ssid = manager?.connectionInfo?.ssid
                    if (ssid != null && Constants.WIFI_PROXY_NAMES.contains(ssid)) {
                        return true
                    }
                }
            }
            return false
        } else {
            val ssid = wifiManager?.connectionInfo?.ssid
            if (ssid != null) {
                if (Constants.WIFI_PROXY_NAMES.contains(ssid)) {
                    return true
                }
            }
            return false
        }
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
    override fun aLogout() {
        SagresTaskExecutor.getIOThreadExecutor().execute {
            logout()
        }
    }

    @WorkerThread
    override fun logout() {
        database.clearAllTables()
        cookieJar.clear()
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aLogin(username: String, password: String): LiveData<LoginCallback> {
        return LoginOperation(username, password, SagresTaskExecutor.getNetworkThreadExecutor()).result
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun login(username: String, password: String): LoginCallback {
        return LoginOperation(username, password, null).finishedResult
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aMe(): LiveData<PersonCallback> {
        return PersonOperation(null, SagresTaskExecutor.getNetworkThreadExecutor(), false).result
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun me(): PersonCallback {
        return PersonOperation(null, null, false).finishedResult
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aMessages(userId: Long, fetchAll: Boolean): LiveData<MessagesCallback> {
        return MessagesOperation(SagresTaskExecutor.getNetworkThreadExecutor(), userId, fetchAll).result
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun messages(userId: Long, fetchAll: Boolean): MessagesCallback {
        return MessagesOperation(null, userId, fetchAll).finishedResult
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aMessagesHtml(): LiveData<MessagesCallback> {
        return OldMessagesOperation(SagresTaskExecutor.getNetworkThreadExecutor()).result
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun messagesHtml(): MessagesCallback {
        return OldMessagesOperation(SagresTaskExecutor.getNetworkThreadExecutor()).finishedResult
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aCalendar(): LiveData<CalendarCallback> {
        return CalendarOperation(SagresTaskExecutor.getNetworkThreadExecutor()).result
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun calendar(): CalendarCallback {
        return CalendarOperation(SagresTaskExecutor.getNetworkThreadExecutor()).finishedResult
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aSemesters(userId: Long): LiveData<SemesterCallback> {
        return SemesterOperation(SagresTaskExecutor.getNetworkThreadExecutor(), userId).result
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun semesters(userId: Long): SemesterCallback {
        return SemesterOperation(null, userId).finishedResult
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aStartPage(): LiveData<StartPageCallback> {
        return StartPageOperation(SagresTaskExecutor.getNetworkThreadExecutor()).result
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun startPage(): StartPageCallback {
        return StartPageOperation(null).finishedResult
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aGetCurrentGrades(): LiveData<GradesCallback> {
        return GradesOperation(null, null, SagresTaskExecutor.getNetworkThreadExecutor()).result
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun getCurrentGrades(): GradesCallback {
        return GradesOperation(null, null, null).finishedResult
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun getGradesFromSemester(semesterSagresId: Long, document: Document): GradesCallback {
        return GradesOperation(semesterSagresId, document, null).finishedResult
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun downloadEnrollment(file: File): DocumentCallback {
        return DocumentOperation(file, Constants.SAGRES_ENROLL_CERT, null).finishedResult
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun downloadFlowchart(file: File): DocumentCallback {
        return DocumentOperation(file, Constants.SAGRES_FLOWCHART, null).finishedResult
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun downloadHistory(file: File): DocumentCallback {
        return DocumentOperation(file, Constants.SAGRES_HISTORY, null).finishedResult
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun loadDisciplineDetails(semester: String?, code: String?, group: String?): DisciplineDetailsCallback {
        return DisciplineDetailsOperation(semester, code, group, null).finishedResult
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aLoadDisciplineDetails(semester: String?, code: String?, group: String?): LiveData<DisciplineDetailsCallback> {
        return DisciplineDetailsOperation(semester, code, group, SagresTaskExecutor.getNetworkThreadExecutor()).result
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aLoadDemandOffers(): LiveData<DemandOffersCallback> {
        return LoadDemandOffersOperation(SagresTaskExecutor.getNetworkThreadExecutor()).result
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun loadDemandOffers(): DemandOffersCallback {
        return LoadDemandOffersOperation(null).finishedResult
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun createDemandOffer(offers: List<SDemandOffer>): DemandCreatorCallback {
        return CreateDemandOperation(offers, null).finishedResult
    }

    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun aGetRequestedServices(login: Boolean): LiveData<RequestedServicesCallback> {
        return RequestedServicesOperation(SagresTaskExecutor.getNetworkThreadExecutor(), login).result
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun getRequestedServices(login: Boolean): RequestedServicesCallback {
        return RequestedServicesOperation(null, login).finishedResult
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
