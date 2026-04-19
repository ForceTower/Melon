package dev.forcetower.melon.core.session.domain.model

sealed class AuthState {
    data class Authenticated(val user: User) : AuthState()
    data object Unauthenticated : AuthState()
}
