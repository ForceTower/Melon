package dev.forcetower.melon.core.session.domain.model

// Plain-text upstream portal credentials persisted alongside the session.
// `updatedAt` is the ISO-8601 timestamp of the most recent successful login
// that wrote the row — kept here so callers reading the credentials don't
// have to also reach into the database entity for diagnostics.
data class UserCredentials(
    val username: String,
    val password: String,
    val updatedAt: String,
)
