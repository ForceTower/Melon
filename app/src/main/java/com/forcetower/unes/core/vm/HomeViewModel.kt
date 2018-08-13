package com.forcetower.unes.core.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import com.forcetower.unes.core.model.Access
import com.forcetower.unes.core.model.Message
import com.forcetower.unes.core.model.Profile
import com.forcetower.unes.core.storage.repository.SagresDataRepository
import com.forcetower.unes.core.storage.repository.UserRepository
import javax.inject.Inject

class HomeViewModel
@Inject constructor(
        private val userRepository: UserRepository,
        private val dataRepository: SagresDataRepository
): ViewModel() {
    val access: LiveData<Access?> by lazy { userRepository.getAccess() }
    val profile: LiveData<Profile> by lazy { userRepository.getProfileMe() }
    val messages: LiveData<PagedList<Message>> by lazy { dataRepository.getMessages() }
}
