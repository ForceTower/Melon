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