package dev.forcetower.melon.core.session.domain

import dev.forcetower.melon.core.network.AuthTokenSource
import dev.forcetower.melon.core.session.domain.model.AuthState
import dev.forcetower.melon.core.session.domain.model.User
import dev.forcetower.melon.core.session.domain.model.UserCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SessionStore : AuthTokenSource {
    val authState: StateFlow<AuthState>

    /**
     * Persists session tokens and the authenticated user.
     *
     * `username` and `password` are the upstream (Snowpiercer) credentials —
     * they're cached in plain text so background syncs can re-authenticate
     * without prompting the user. Pass `null` for both when the login path
     * doesn't yield upstream credentials (e.g. passkey assertion). Background
     * Snowpiercer re-auth will be unavailable for that session until the user
     * logs in again with username + password.
     */
    suspend fun persist(
        accessToken: String,
        refreshToken: String,
        user: User,
        username: String? = null,
        password: String? = null,
    )
    suspend fun getCredentials(): UserCredentials?
    fun observeCredentials(): Flow<UserCredentials?>

    /**
     * Upserts only the cached upstream (Snowpiercer) credentials for the
     * currently authenticated user. Used after a passkey login to backfill
     * the credentials row from the server during the initial mirror sync,
     * so background syncs have something to re-authenticate with. No-op if
     * there is no current user (called outside an authenticated session).
     */
    suspend fun updateUpstreamCredentials(username: String, password: String)

    suspend fun logout()
}
