package dev.forcetower.melon.feature.auth.domain.repository

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.auth.domain.model.LoginError

internal interface AuthRepository {
    suspend fun login(username: String, password: String): Outcome<Unit, LoginError>
}
