package dev.forcetower.melon.feature.auth

sealed class Outcome<out T> {
    data class Ok<T>(val value: T) : Outcome<T>()
    data class Err(val error: DomainError) : Outcome<Nothing>()
}
