package dev.forcetower.melon.feature.auth

sealed class DomainError {
    data object Network : DomainError()
    data object InvalidCredentials : DomainError()
    data class Server(val message: String?) : DomainError()
    data object Unknown : DomainError()
}
