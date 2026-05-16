package dev.forcetower.melon.feature.auth.domain.repository

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.session.domain.model.User
import dev.forcetower.melon.feature.auth.domain.model.LoginError
import dev.forcetower.melon.feature.auth.domain.model.PasskeyAssertion
import dev.forcetower.melon.feature.auth.domain.model.PasskeyChallenge

internal interface AuthRepository {
    suspend fun login(username: String, password: String): Outcome<User, LoginError>

    suspend fun beginPasskeyLogin(username: String?): Outcome<PasskeyChallenge, LoginError>

    suspend fun completePasskeyLogin(
        sessionId: String,
        assertion: PasskeyAssertion,
    ): Outcome<User, LoginError>
}
