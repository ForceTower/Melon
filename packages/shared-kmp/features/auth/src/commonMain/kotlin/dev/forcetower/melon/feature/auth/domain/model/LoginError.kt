package dev.forcetower.melon.feature.auth.domain.model

sealed interface LoginError {
    enum class Kind : LoginError {
        NoConnection,
        InvalidCredentials,
        Unexpected,
    }

    data class Server(val message: String?) : LoginError
}
