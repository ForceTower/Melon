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

package com.forcetower.uefs.feature.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.sagres.operation.Callback
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.core.storage.repository.LoginSagresRepository
import javax.inject.Inject

class LoginViewModel @Inject constructor(private val repository: LoginSagresRepository) : ViewModel() {
    private var loginSrc: MediatorLiveData<Callback> = MediatorLiveData()
    private var loginRunning: Boolean = false
    private var connected: Boolean = false

    fun getAccess() = repository.getAccess()

    fun getStep() = repository.currentStep

    fun login(username: String, password: String, captcha: String?, deleteDatabase: Boolean = false, skipLogin: Boolean = false) {
        if (!loginRunning) {
            val login = repository.login(username, password, captcha, deleteDatabase, skipLogin)
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
                if (!loginRunning) {
                    loginSrc.removeSource(login)
                }
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