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

package com.forcetower.unes.core.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.sagres.operation.Callback
import com.forcetower.sagres.operation.Status
import com.forcetower.unes.core.storage.repository.LoginSagresRepository
import javax.inject.Inject

class LoginViewModel @Inject constructor(private val repository: LoginSagresRepository): ViewModel() {
    private var loginSrc : MediatorLiveData<Callback> = MediatorLiveData()
    private var loginRunning: Boolean = false
    private var connected: Boolean = false

    fun getAccess() = repository.getAccess()

    fun getStep() = repository.currentStep

    fun login(username: String, password: String, deleteDatabase: Boolean = false) {
        if (!loginRunning) {
            val login = repository.login(username, password, deleteDatabase)
            loginRunning = true
            loginSrc.addSource(login) {
                loginRunning = when (it.status) {
                    Status.INVALID_LOGIN -> false
                    Status.NETWORK_ERROR -> false
                    Status.RESPONSE_FAILED -> false
                    Status.SUCCESS -> false
                    Status.APPROVAL_ERROR -> false
                    else -> true
                }
                loginSrc.value = it
            }
        }
    }

    fun getLogin(): LiveData<Callback> = loginSrc

    fun getProfile() = repository.getProfileMe()

    fun setConnected() {
        connected = true
    }

    fun isConnected() = connected

    override fun onCleared() {
        super.onCleared()
        loginRunning = false
        if (loginRunning) repository.stopCurrentLogin()
    }
}