package dev.forcetower.melon.feature.auth

import dev.zacsweers.metro.Inject

@Inject
class LoginUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(username: String, password: String): Outcome<Unit> =
        repository.login(username, password)
}
