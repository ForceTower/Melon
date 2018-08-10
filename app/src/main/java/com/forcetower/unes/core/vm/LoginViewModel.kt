package com.forcetower.unes.core.vm

import androidx.lifecycle.ViewModel
import com.forcetower.unes.core.storage.repository.UserRepository
import javax.inject.Inject

class LoginViewModel @Inject constructor(private val repository: UserRepository): ViewModel() {

    fun getAccess() = repository.getAccess()
}