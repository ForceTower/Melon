package com.forcetower.unes.core.vm

import androidx.lifecycle.ViewModel
import com.forcetower.unes.core.storage.repository.UserRepository
import javax.inject.Inject

class HomeViewModel @Inject constructor(private val repository: UserRepository): ViewModel() {
    fun getAccess() = repository.getAccess()
    fun getProfile() = repository.getProfileMe()
}
