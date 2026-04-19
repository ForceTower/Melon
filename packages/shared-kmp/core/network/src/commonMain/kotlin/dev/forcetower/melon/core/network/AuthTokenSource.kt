package dev.forcetower.melon.core.network

// Indirection so core/network can attach a bearer token to every request without depending on
// core/session. Implemented in core/session where the token actually lives.
interface AuthTokenSource {
    suspend fun getAccessToken(): String?
}
