package dev.forcetower.melon.feature.auth.data.repository

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.auth.domain.model.LoginError
import dev.forcetower.melon.feature.auth.data.network.AuthService
import dev.forcetower.melon.feature.auth.data.dto.LoginRequest
import dev.forcetower.melon.feature.auth.data.dto.LoginResponse
import dev.forcetower.melon.feature.auth.data.mapper.toDomain
import dev.forcetower.melon.feature.auth.domain.repository.AuthRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class AuthRepositoryImpl(
    private val api: AuthService,
    private val sessionStore: SessionStore,
) : AuthRepository {

    override suspend fun login(username: String, password: String): Outcome<Unit, LoginError> = try {
        val response = api.login(LoginRequest(username, password))
        handleLoginResponse(response)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: SerializationException) {
        Outcome.Err(LoginError.Kind.Unexpected)
    } catch (_: Throwable) {
        Outcome.Err(LoginError.Kind.NoConnection)
    }

    private suspend fun handleLoginResponse(response: HttpResponse): Outcome<Unit, LoginError> {
        val status = response.status.value
        return when (status) {
            in 200..299 -> persistSession(response.body())
            400 -> Outcome.Err(LoginError.Kind.InvalidCredentials)
            in 500..599 -> {
                val envelope = runCatching { response.body<ApiEnvelope<LoginResponse>>() }.getOrNull()
                Outcome.Err(LoginError.Server(envelope?.message))
            }
            else -> Outcome.Err(LoginError.Kind.Unexpected)
        }
    }

    private suspend fun persistSession(envelope: ApiEnvelope<LoginResponse>): Outcome<Unit, LoginError> {
        val payload = envelope.data ?: return Outcome.Err(LoginError.Kind.Unexpected)
        sessionStore.persist(
            accessToken = payload.accessToken,
            refreshToken = payload.refreshToken,
            user = payload.user.toDomain(),
        )
        return Outcome.Ok(Unit)
    }
}
