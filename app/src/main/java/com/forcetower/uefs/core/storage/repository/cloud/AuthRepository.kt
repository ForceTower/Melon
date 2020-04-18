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

package com.forcetower.uefs.core.storage.repository.cloud

import android.content.SharedPreferences
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.annotations.USLoginMethod
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.model.unes.AccessToken
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.network.adapter.ApiResponse
import com.forcetower.uefs.core.storage.network.adapter.asLiveData
import com.forcetower.uefs.core.storage.resource.NetworkBoundResource
import com.forcetower.uefs.core.storage.resource.Resource
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val executors: AppExecutors,
    private val database: UDatabase,
    private val service: UService,
    private val preferences: SharedPreferences
) {
    @MainThread
    fun login(
        username: String,
        password: String,
        @USLoginMethod method: String = LOGIN_METHOD_UNES,
        forcedFetch: Boolean = true
    ): LiveData<Resource<AccessToken?>> {
        return object : NetworkBoundResource<AccessToken?, AccessToken> (executors) {
            override fun loadFromDb() = database.accessTokenDao().getAccessToken()
            override fun shouldFetch(it: AccessToken?) = forcedFetch || it == null
            override fun createCall(): LiveData<ApiResponse<AccessToken>> {
                return when (method) {
                    LOGIN_METHOD_UNES -> service.login(username, password).asLiveData()
                    else -> service.loginWithSagres(username, password).asLiveData()
                }
            }
            override fun saveCallResult(value: AccessToken) { database.accessTokenDao().insert(value) }
        }.asLiveData()
    }

    @MainThread
    fun autoLogin(): LiveData<Resource<AccessToken?>> {
        val result = MediatorLiveData<Resource<AccessToken?>>()
        val accessSrc = database.accessDao().getAccess()
        result.addSource(accessSrc) {
            result.removeSource(accessSrc)
            if (it != null) {
                val loginSrc = login(it.username, it.password, LOGIN_METHOD_SAGRES)
                result.addSource(loginSrc) { res ->
                    result.value = res
                }
            } else {
                result.value = Resource.error("Invalid access", null)
            }
        }
        return result
    }

    @AnyThread
    fun performAccountSyncStateIfNeededAsync() {
        executors.networkIO().execute {
            val tk = database.accessTokenDao().getAccessTokenDirect()
            if (tk == null) performAccountSyncState()
            else updateAccount()
        }
    }

    @WorkerThread
    private fun updateAccount() {
        try {
            val response = service.getAccount().execute()
            if (response.isSuccessful) {
                val account = response.body()!!
                database.accountDao().insert(account)
            }
        } catch (t: Throwable) {}
    }

    @WorkerThread
    fun performAccountSyncState() {
        val access = database.accessDao().getAccessDirect()
        access ?: return
        if (!access.valid) return
        val token = syncLogin(access.username, access.password)
        token ?: return
        syncCredentials(access)

        val profile = database.profileDao().selectMeDirect()
        profile ?: return
        syncProfileState(profile)
        updateAccount()
    }

    @WorkerThread
    private fun syncProfileState(profile: Profile): Boolean {
        try {
            val response = service.setupProfile(profile).execute()
            return response.isSuccessful
        } catch (t: Throwable) {
            Timber.e(t)
        }
        return false
    }

    @WorkerThread
    private fun syncCredentials(access: Access): Boolean {
        try {
            // Triggers a on server sync state
            val response = service.setupAccount(access).execute()
            return response.isSuccessful
        } catch (t: Throwable) {
            Timber.e(t)
        }
        return false
    }

    @WorkerThread
    fun syncLogin(username: String, password: String): AccessToken? {
        try {
            Timber.d("Sign in using ${username.trim()} and $password")
            val response = service.login(username.trim(), password).execute()
            if (response.isSuccessful) {
                val token = response.body()
                if (token != null) {
                    database.accessTokenDao().insert(token)
                    return token
                } else {
                    Timber.e("Token response is null")
                }
            } else {
                Timber.e("Failed with code: ${response.code()}")
            }
        } catch (t: Throwable) {
            Timber.e(t)
        }
        return null
    }

    fun getAccessToken() = database.accessTokenDao().getAccessToken()
    fun getAccessTokenDirect() = database.accessTokenDao().getAccessTokenDirect()

    companion object {
        const val LOGIN_METHOD_UNES = "password"
        const val LOGIN_METHOD_SAGRES = "sagres"
    }
}