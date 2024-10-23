package com.forcetower.uefs.domain.usecase.auth

import com.forcetower.uefs.core.model.unes.EdgeServiceAccount
import com.forcetower.uefs.core.storage.repository.cloud.EdgeAuthRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class EdgeAnonymousLoginUseCase @Inject constructor(
    private val repository: EdgeAuthRepository
) {
    suspend fun prepareAndLogin() {
        return repository.prepareAndLogin()
    }

    suspend fun loginOrThrow(): EdgeServiceAccount? {
        return repository.doAnonymousLogin()
    }

    suspend fun invoke(username: String, password: String) {
        repository.anonymous(username, password)
    }
}
