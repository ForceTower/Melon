package dev.forcetower.melon.core.session

import dev.forcetower.melon.core.network.AuthTokenSource
import kotlinx.coroutines.flow.StateFlow

interface SessionStore : AuthTokenSource {
    val authState: StateFlow<AuthState>
    suspend fun persist(accessToken: String, refreshToken: String, user: User)
    suspend fun logout()
}
