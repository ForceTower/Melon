package com.forcetower.uefs.core.vm

import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.storage.repository.ProfileRepository
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
    val repository: ProfileRepository
): ViewModel(){
    fun getProfile() = repository.getMeProfile()
}