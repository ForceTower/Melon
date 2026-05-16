package dev.forcetower.melon.feature.auth.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.session.domain.model.User
import dev.forcetower.melon.feature.auth.domain.model.LoginError
import dev.forcetower.melon.feature.auth.domain.repository.AuthRepository
import dev.zacsweers.metro.Inject

@Inject
class LoginUseCase internal constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(username: String, password: String): Outcome<User, LoginError> {
        return repository.login(username, password)
    }
}
