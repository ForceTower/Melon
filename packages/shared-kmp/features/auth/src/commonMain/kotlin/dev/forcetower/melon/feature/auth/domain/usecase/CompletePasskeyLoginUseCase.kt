package dev.forcetower.melon.feature.auth.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.session.domain.model.User
import dev.forcetower.melon.feature.auth.domain.model.LoginError
import dev.forcetower.melon.feature.auth.domain.model.PasskeyAssertion
import dev.forcetower.melon.feature.auth.domain.repository.AuthRepository
import dev.zacsweers.metro.Inject

@Inject
class CompletePasskeyLoginUseCase internal constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        sessionId: String,
        assertion: PasskeyAssertion,
    ): Outcome<User, LoginError> {
        return repository.completePasskeyLogin(sessionId, assertion)
    }
}
