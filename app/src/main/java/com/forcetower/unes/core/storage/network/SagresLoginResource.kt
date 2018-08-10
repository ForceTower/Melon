package com.forcetower.unes.core.storage.network

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.forcetower.unes.AppExecutors
import com.forcetower.unes.R
import com.forcetower.unes.core.parsers.SagresBasicParser
import com.forcetower.unes.core.storage.network.adapter.adapt
import com.forcetower.unes.core.storage.resource.Resource
import com.forcetower.unes.core.storage.resource.SagresResponse
import okhttp3.Call

@MainThread
abstract class SagresLoginResource(private val executors: AppExecutors) {
    private val result: MediatorLiveData<Resource<Int>> = MediatorLiveData()

    private var login: LoginContinuation = object: LoginContinuation() {
        override fun execute() {

        }
    }

    init { syncLogin() }

    private fun syncLogin() {
        val call = createLoginCall()
        val loginSrc = call.adapt()
        result.addSource(loginSrc) { login ->
            result.removeSource(loginSrc)
            loginStep(login)
        }
    }

    private fun loginStep(login: SagresResponse?) {
        if (login != null && login.isSuccessful()) {
            val document = login.document
            if (SagresBasicParser.isConnected(document)) {

            } else {
                result.value = Resource.error("Login Failed", 401, R.string.error_login_failed)
            }
        } else {
            //result.value = Resource.error("Connection Error", login?.code, R.string.error_connection_failed)
        }

    }

    abstract fun createLoginCall(): Call

    fun asLiveData(): LiveData<Resource<Int>> = result

    private abstract class LoginContinuation {
        abstract fun execute()
    }
}