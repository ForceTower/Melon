package dev.forcetower.melon.feature.auth

interface AuthRepository {
    suspend fun login(username: String, password: String): Outcome<Unit>
}
