package com.forcetower.unes.core.storage.resource

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

abstract class LoginSagresResource {
    private val result: MediatorLiveData<Int> = MediatorLiveData()

    fun syncLogin(username: String, password: String) {

    }

    fun asLiveData(): LiveData<Int> = result
}