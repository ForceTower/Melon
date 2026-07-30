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
     * True once `api/auth/token/refresh` has terminally rejected the stored
     * pair — the session cannot be recovered without a fresh login.
     *
     * Deliberately separate from [authState]: an expired token is still a
     * token on disk, so the user stays `Authenticated` and keeps reading the
     * local mirror. Splash routing and analytics identity are unaffected;
     * only the "Sessão expirada" banner keys off this.
     */
    val sessionInvalid: StateFlow<Boolean>

    /**
     * True once the server reports the stored SAGRES password no longer works.
     * Deliberately separate from [sessionInvalid]: the Melon session and the
     * upstream credential fail independently and the user fixes them in
     * different ways. Persisted so the banner renders on a cold start offline.
     */
    val credentialsInvalid: StateFlow<Boolean>

    /** SAGRES username from `api/me/status`; the re-auth sheet shows it read-only. */
    val upstreamUsername: StateFlow<String?>

    /**
     * Resolves the auth state by reading persisted state directly, bypassing
     * the [authState] StateFlow's startup race. The flow seeds with
     * [AuthState.Unauthenticated] so `.first()` on a cold start can hand back
     * the seed before the implementation finishes loading the token from
     * disk. Splash routing reads this instead so a returning user lands on
     * Home rather than being bounced back to onboarding.
     */
    suspend fun currentAuthState(): AuthState

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
    suspend fun getRefreshToken(): String?

    /**
     * Swaps both halves of the token pair in place, leaving the user row,
     * cached upstream credentials and the whole local mirror untouched.
     * `api/auth/token/refresh` returns no user, so [persist] would have
     * nothing to write there.
     */
    suspend fun replaceTokens(accessToken: String, refreshToken: String)

    suspend fun setSessionInvalid(invalid: Boolean)

    suspend fun setCredentialsInvalid(invalid: Boolean)

    suspend fun setUpstreamUsername(username: String)

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
