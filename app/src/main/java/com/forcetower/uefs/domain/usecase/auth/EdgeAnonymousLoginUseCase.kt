package com.forcetower.uefs.domain.usecase.auth

import com.forcetower.uefs.core.storage.repository.cloud.EdgeAuthRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class EdgeAnonymousLoginUseCase @Inject constructor(
    private val repository: EdgeAuthRepository
) {
    suspend fun prepareAndLogin() {
        repository.prepareAndLogin()
    }

    suspend fun invoke(username: String, password: String) {
        repository.anonymous(username, password)
    }
}