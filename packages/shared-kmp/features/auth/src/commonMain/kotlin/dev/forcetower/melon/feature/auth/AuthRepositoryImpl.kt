package dev.forcetower.melon.feature.auth

import dev.forcetower.melon.core.session.SessionStore
import dev.forcetower.melon.feature.auth.data.ApiEnvelope
import dev.forcetower.melon.feature.auth.data.AuthApi
import dev.forcetower.melon.feature.auth.data.LoginRequest
import dev.forcetower.melon.feature.auth.data.LoginResponse
import dev.forcetower.melon.feature.auth.data.toDomain
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

internal class AuthRepositoryImpl(
    private val api: AuthApi,
    private val sessionStore: SessionStore,
) : AuthRepository {

    override suspend fun login(username: String, password: String): Outcome<Unit> = try {
        val response = api.login(LoginRequest(username, password))
        handleLoginResponse(response)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: SerializationException) {
        Outcome.Err(DomainError.Unknown)
    } catch (_: Throwable) {
        Outcome.Err(DomainError.Network)
    }

    private suspend fun handleLoginResponse(response: HttpResponse): Outcome<Unit> {
        val status = response.status.value
        return when (status) {
            in 200..299 -> persistSession(response.body())
            400 -> Outcome.Err(DomainError.InvalidCredentials)
            in 500..599 -> {
                val envelope = runCatching { response.body<ApiEnvelope<LoginResponse>>() }.getOrNull()
                Outcome.Err(DomainError.Server(envelope?.message))
            }
            else -> Outcome.Err(DomainError.Unknown)
        }
    }

    private suspend fun persistSession(envelope: ApiEnvelope<LoginResponse>): Outcome<Unit> {
        val payload = envelope.data ?: return Outcome.Err(DomainError.Unknown)
        sessionStore.persist(
            accessToken = payload.accessToken,
            refreshToken = payload.refreshToken,
            user = payload.user.toDomain(),
        )
        return Outcome.Ok(Unit)
    }
}
