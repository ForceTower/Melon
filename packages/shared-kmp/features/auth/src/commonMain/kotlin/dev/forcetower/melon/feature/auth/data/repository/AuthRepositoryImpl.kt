package dev.forcetower.melon.feature.auth.data.repository

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.session.domain.model.User
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
    logger: Logger,
) : AuthRepository {

    private val log = logger.withTag("AuthRepositoryImpl")

    override suspend fun login(username: String, password: String): Outcome<User, LoginError> = try {
        log.i { "login attempt username=$username" }
        val response = api.login(LoginRequest(username, password))
        return handleLoginResponse(response)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (ex: SerializationException) {
        log.e(throwable = ex) { "login failed: envelope deserialization" }
        Outcome.Err(LoginError.Kind.Unexpected)
    } catch (ex: Throwable) {
        log.w(throwable = ex) { "login failed: transport" }
        Outcome.Err(LoginError.Kind.NoConnection)
    }

    private suspend fun handleLoginResponse(response: HttpResponse): Outcome<User, LoginError> {
        val status = response.status.value
        return when (status) {
            in 200..299 -> persistSession(response.body())
            400 -> {
                log.w { "login rejected: invalid credentials" }
                Outcome.Err(LoginError.Kind.InvalidCredentials)
            }
            in 500..599 -> {
                val envelope = runCatching { response.body<ApiEnvelope<LoginResponse>>() }.getOrNull()
                log.w { "login server $status message=${envelope?.message ?: "<none>"}" }
                Outcome.Err(LoginError.Server(envelope?.message))
            }
            else -> {
                log.w { "login unexpected status $status" }
                Outcome.Err(LoginError.Kind.Unexpected)
            }
        }
    }

    private suspend fun persistSession(envelope: ApiEnvelope<LoginResponse>): Outcome<User, LoginError> {
        val payload = envelope.data ?: run {
            log.w { "login 2xx with null data (message=${envelope.message})" }
            return Outcome.Err(LoginError.Kind.Unexpected)
        }
        val user = payload.user.toDomain()
        sessionStore.persist(
            accessToken = payload.accessToken,
            refreshToken = payload.refreshToken,
            user = user,
        )
        log.i { "login ok userId=${user.id}" }
        return Outcome.Ok(user)
    }
}
