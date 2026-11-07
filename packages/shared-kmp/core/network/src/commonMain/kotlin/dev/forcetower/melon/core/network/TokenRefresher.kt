package dev.forcetower.melon.core.network

// Indirection so core/network can rotate a spent token without depending on
// core/session. Implemented in core/session where the refresh token lives.
interface TokenRefresher {
    /**
     * Exchanges the spent token pair for a fresh one and persists it.
     *
     * [staleAccessToken] is the token the failing request actually carried, so
     * a caller that lost a race can tell that someone else already rotated
     * instead of burning the new pair a second time. Returns true only when the
     * stored access token is the rotation of [staleAccessToken], so the request
     * is worth retrying under it; never for a token from a later login.
     */
    suspend fun refresh(staleAccessToken: String): Boolean
}
