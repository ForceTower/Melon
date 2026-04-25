package dev.forcetower.melon.core.session.domain

import dev.forcetower.melon.core.network.AuthTokenSource
import dev.forcetower.melon.core.session.domain.model.AuthState
import dev.forcetower.melon.core.session.domain.model.User
import dev.forcetower.melon.core.session.domain.model.UserCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SessionStore : AuthTokenSource {
    val authState: StateFlow<AuthState>
    suspend fun persist(
        accessToken: String,
        refreshToken: String,
        user: User,
        username: String,
        password: String,
    )
    suspend fun getCredentials(): UserCredentials?
    fun observeCredentials(): Flow<UserCredentials?>
    suspend fun logout()
}
