package com.forcetower.unes.core.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.sagres.operation.Callback
import com.forcetower.sagres.operation.login.LoginCallback
import com.forcetower.sagres.operation.Status
import com.forcetower.unes.core.storage.repository.UserRepository
import javax.inject.Inject

class LoginViewModel @Inject constructor(private val repository: UserRepository): ViewModel() {
    private var loginSrc : MediatorLiveData<Callback> = MediatorLiveData()
    private var loginRunning: Boolean = false
    private var connected: Boolean = false

    fun getAccess() = repository.getAccess()

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