package dev.forcetower.melon.feature.auth.data.repository

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.core.network.NetworkError
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.session.domain.model.User
import dev.forcetower.melon.feature.auth.data.dto.LoginRequest
import dev.forcetower.melon.feature.auth.data.dto.LoginResponse
import dev.forcetower.melon.feature.auth.data.dto.PasskeyAuthOptionsRequest
import dev.forcetower.melon.feature.auth.data.dto.PasskeyAuthOptionsResponse
import dev.forcetower.melon.feature.auth.data.dto.PasskeyAuthVerifyRequest
import dev.forcetower.melon.feature.auth.data.mapper.toDomain
import dev.forcetower.melon.feature.auth.data.mapper.toDto
import dev.forcetower.melon.feature.auth.data.network.AuthService
import dev.forcetower.melon.feature.auth.domain.model.LoginError
import dev.forcetower.melon.feature.auth.domain.model.PasskeyAssertion
import dev.forcetower.melon.feature.auth.domain.model.PasskeyChallenge
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
        handleLoginResponse(response, username, password)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (ex: NetworkError.Tls) {
        log.w(throwable = ex) { "login failed: TLS — ${ex.message}" }
        Outcome.Err(ex.toLoginError())
    } catch (ex: SerializationException) {
        log.e(throwable = ex) { "login failed: envelope deserialization" }
        Outcome.Err(LoginError.Kind.Unexpected)
    } catch (ex: Throwable) {
        log.w(throwable = ex) { "login failed: transport" }
        Outcome.Err(LoginError.Kind.NoConnection)
    }

    override suspend fun beginPasskeyLogin(username: String?): Outcome<PasskeyChallenge, LoginError> = try {
        log.i { "passkey begin username=${username ?: "<discoverable>"}" }
        val response = api.passkeyAuthOptions(PasskeyAuthOptionsRequest(username))
        when (val status = response.status.value) {
            in 200..299 -> {
                val envelope = response.body<ApiEnvelope<PasskeyAuthOptionsResponse>>()
                val payload = envelope.data ?: run {
                    log.w { "passkey begin 2xx with null data" }
                    return Outcome.Err(LoginError.Kind.Unexpected)
                }
                Outcome.Ok(payload.toDomain())
            }
            in 500..599 -> {
                val envelope = runCatching { response.body<ApiEnvelope<PasskeyAuthOptionsResponse>>() }.getOrNull()
                log.w { "passkey begin server $status message=${envelope?.message ?: "<none>"}" }
                Outcome.Err(LoginError.Server(envelope?.message))
            }
            else -> {
                log.w { "passkey begin unexpected status $status" }
                Outcome.Err(LoginError.Kind.Unexpected)
            }
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (ex: NetworkError.Tls) {
        log.w(throwable = ex) { "passkey begin failed: TLS — ${ex.message}" }
        Outcome.Err(ex.toLoginError())
    } catch (ex: SerializationException) {
        log.e(throwable = ex) { "passkey begin failed: deserialization" }
        Outcome.Err(LoginError.Kind.Unexpected)
    } catch (ex: Throwable) {
        log.w(throwable = ex) { "passkey begin failed: transport" }
        Outcome.Err(LoginError.Kind.NoConnection)
    }

    override suspend fun completePasskeyLogin(
        sessionId: String,
        assertion: PasskeyAssertion,
    ): Outcome<User, LoginError> = try {
        log.i { "passkey complete sessionId=$sessionId" }
        val response = api.passkeyAuthVerify(
            PasskeyAuthVerifyRequest(sessionId = sessionId, response = assertion.toDto()),
        )
        when (val status = response.status.value) {
            in 200..299 -> persistPasskeySession(response.body())
            401 -> {
                log.w { "passkey complete: rejected by server" }
                Outcome.Err(LoginError.Kind.InvalidCredentials)
            }
            in 500..599 -> {
                val envelope = runCatching { response.body<ApiEnvelope<LoginResponse>>() }.getOrNull()
                log.w { "passkey complete server $status message=${envelope?.message ?: "<none>"}" }
                Outcome.Err(LoginError.Server(envelope?.message))
            }
            else -> {
                log.w { "passkey complete unexpected status $status" }
                Outcome.Err(LoginError.Kind.Unexpected)
            }
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (ex: NetworkError.Tls) {
        log.w(throwable = ex) { "passkey complete failed: TLS — ${ex.message}" }
        Outcome.Err(ex.toLoginError())
    } catch (ex: SerializationException) {
        log.e(throwable = ex) { "passkey complete failed: deserialization" }
        Outcome.Err(LoginError.Kind.Unexpected)
    } catch (ex: Throwable) {
        log.w(throwable = ex) { "passkey complete failed: transport" }
        Outcome.Err(LoginError.Kind.NoConnection)
    }

    private suspend fun handleLoginResponse(
        response: HttpResponse,
        username: String,
        password: String,
    ): Outcome<User, LoginError> {
        val status = response.status.value
        return when (status) {
            in 200..299 -> persistSession(response.body(), username, password)
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

    private suspend fun persistSession(
        envelope: ApiEnvelope<LoginResponse>,
        username: String,
        password: String,
    ): Outcome<User, LoginError> {
        val payload = envelope.data ?: run {
            log.w { "login 2xx with null data (message=${envelope.message})" }
            return Outcome.Err(LoginError.Kind.Unexpected)
        }
        val user = payload.user.toDomain()
        sessionStore.persist(
            accessToken = payload.accessToken,
            refreshToken = payload.refreshToken,
            user = user,
            username = username,
            password = password,
        )
        log.i { "login ok userId=${user.id}" }
        return Outcome.Ok(user)
    }

    // Passkey login doesn't yield upstream Snowpiercer credentials, so we
    // persist the session without caching them. Background Snowpiercer
    // re-auth will be unavailable until the user logs in with username +
    // password at least once.
    private suspend fun persistPasskeySession(
        envelope: ApiEnvelope<LoginResponse>,
    ): Outcome<User, LoginError> {
        val payload = envelope.data ?: run {
            log.w { "passkey 2xx with null data (message=${envelope.message})" }
            return Outcome.Err(LoginError.Kind.Unexpected)
        }
        val user = payload.user.toDomain()
        sessionStore.persist(
            accessToken = payload.accessToken,
            refreshToken = payload.refreshToken,
            user = user,
        )
        log.i { "passkey ok userId=${user.id}" }
        return Outcome.Ok(user)
    }
}

private fun NetworkError.Tls.toLoginError(): LoginError = when (this) {
    is NetworkError.Tls.Intercepted -> LoginError.TlsIntercepted(displayName)
    is NetworkError.Tls.ClockSkew -> LoginError.TlsClockSkew
    is NetworkError.Tls.Generic -> LoginError.TlsGeneric
}
