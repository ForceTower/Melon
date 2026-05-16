package dev.forcetower.melon.feature.notifications.domain.model

sealed interface NotificationTokenError {
    enum class Kind : NotificationTokenError {
        Unauthorized,
        NoConnection,
        Unexpected,
    }

    data class Server(val message: String?) : NotificationTokenError
}
