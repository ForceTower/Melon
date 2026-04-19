package dev.forcetower.melon.core.session.domain

import dev.forcetower.melon.core.network.AuthTokenSource
import dev.forcetower.melon.core.session.domain.model.AuthState
import dev.forcetower.melon.core.session.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface SessionStore : AuthTokenSource {
    val authState: StateFlow<AuthState>
    suspend fun persist(accessToken: String, refreshToken: String, user: User)
    suspend fun logout()
}
