package com.forcetower.uefs.core.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.AccessToken
import com.forcetower.uefs.core.storage.repository.cloud.AuthRepository
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.feature.shared.extensions.setValueIfNew
import timber.log.Timber
import javax.inject.Inject

class UnesverseViewModel @Inject constructor(
    private val auth: AuthRepository
) : ViewModel() {
    val access = auth.getAccessToken()

    private val _isLoggingIn = MediatorLiveData<Boolean>()
    val isLoggingIn: LiveData<Boolean>
        get() = _isLoggingIn

    private val _loggingIn = MediatorLiveData<Resource<AccessToken?>>()
    val loggingIn: MediatorLiveData<Resource<AccessToken?>>
        get() = _loggingIn

    private val _loginMessenger = MutableLiveData<Event<Int>>()
    val loginMessenger: LiveData<Event<Int>>
        get() = _loginMessenger

    fun login() {
        if (_isLoggingIn.value == true) return
        _isLoggingIn.value = true
        val source = auth.autoLogin()
        _loggingIn.addSource(source) {
            _loggingIn.value = it
            Timber.d("The current login status: ${it.status}")
            when {
                it.status === Status.LOADING -> {
                    _isLoggingIn.value = true
                }
                it.status === Status.ERROR -> {
                    _loggingIn.removeSource(source)
                    _loginMessenger.setValueIfNew(Event(R.string.failed_to_connect_to_unesverse))
                    _isLoggingIn.value = false
                }
                else -> {
                    _loggingIn.removeSource(source)
                    _loginMessenger.setValueIfNew(Event(R.string.connected_to_the_unesverse))
                    _isLoggingIn.value = false
                }
            }
        }
    }
}