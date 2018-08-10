package com.forcetower.unes.core.storage.repository

import androidx.lifecycle.LiveData
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.operation.LoginCallback
import com.forcetower.unes.AppExecutors
import javax.inject.Inject

class UserRepository @Inject constructor(
        private val executor: AppExecutors
) {

    fun getAccess() = SagresNavigator.getInstance()?.database?.accessDao()?.access

    fun login(username: String, password: String): LiveData<LoginCallback>? {
        return SagresNavigator.getInstance()?.login(username, password)
    }

    fun stopCurrentLogin() {
        SagresNavigator.getInstance()?.stopTags("login")
    }
}
