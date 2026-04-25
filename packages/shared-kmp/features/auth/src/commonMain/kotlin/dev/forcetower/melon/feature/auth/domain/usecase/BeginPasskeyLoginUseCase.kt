package dev.forcetower.melon.feature.auth.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.auth.domain.model.LoginError
import dev.forcetower.melon.feature.auth.domain.model.PasskeyChallenge
import dev.forcetower.melon.feature.auth.domain.repository.AuthRepository
import dev.zacsweers.metro.Inject

@Inject
class BeginPasskeyLoginUseCase internal constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(username: String? = null): Outcome<PasskeyChallenge, LoginError> {
        return repository.beginPasskeyLogin(username)
    }
}
