package dev.forcetower.melon.feature.me.domain.model

// UI-facing pair surfaced by `ObserveCurrentCredentialsUseCase`. `updatedAt`
// is deliberately omitted — the Settings screen only needs the username and
// password — so the domain stays minimal even though the underlying entity
// carries an extra column.
data class UserCredentials(
    val username: String,
    val password: String,
)
