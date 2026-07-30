package dev.forcetower.melon.core.session.data

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.core.network.BaseUrl
import dev.forcetower.melon.core.network.TokenRefresher
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Serializes token rotation against `api/auth/token/refresh`. The endpoint
 * burns the refresh token on first use and hands back a new pair, so two
 * concurrent refreshes would rotate past each other and strand the session.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class TokenRefresherImpl(
    private val sessionStore: SessionStore,
    engine: HttpClientEngine,
    baseUrl: BaseUrl,
    json: Json,
    logger: Logger,
) : TokenRefresher {

    private val log = logger.withTag("TokenRefresher")
    private val mutex = Mutex()

    /** Access token whose pair the server already rejected. Without this latch
     *  a structurally dead session re-attempts the refresh on every request. */
    private var burnedAccessToken: String? = null

    // Plugin-free client on purpose: routing the refresh through the shared
    // one would recurse straight back into the 401 hook that called us.
    private val http = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(DefaultRequest) { url(baseUrl.value) }
    }

    override suspend fun refresh(staleAccessToken: String): Boolean = mutex.withLock {
        val current = sessionStore.getAccessToken()
        when {
            current == null -> false
            // Another caller rotated the pair while this request sat on its 401.
            current != staleAccessToken -> true
            burnedAccessToken == staleAccessToken -> false
            else -> rotate(staleAccessToken)
        }
    }

    private suspend fun rotate(staleAccessToken: String): Boolean {
        val refreshToken = sessionStore.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            log.w { "refresh skipped: session has no refresh token" }
            burn(staleAccessToken)
            return false
        }

        return try {
            val response = http.post("api/auth/token/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(accessToken = staleAccessToken, refreshToken = refreshToken))
            }
            when {
                response.status.isSuccess() -> {
                    val envelope = response.body<ApiEnvelope<RefreshResponse>>()
                    val rotated = envelope.data
                    if (envelope.ok && rotated != null) {
                        sessionStore.replaceTokens(rotated.accessToken, rotated.refreshToken)
                        sessionStore.setSessionInvalid(false)
                        log.i { "token refreshed" }
                        true
                    } else {
                        log.w { "refresh returned an empty envelope, staying retryable" }
                        false
                    }
                }
                // 400 covers a spent, revoked or foreign refresh token — the
                // pair is gone and only a fresh login recovers it.
                response.status.value in 400..499 -> {
                    log.w { "refresh rejected status=${response.status.value}: session needs a new login" }
                    burn(staleAccessToken)
                    false
                }
                // A 5xx is the server's problem, so leave the pair retryable.
                else -> {
                    log.w { "refresh failed status=${response.status.value}, staying retryable" }
                    false
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            // Transport or decode failure — deliberately not burned so the
            // next 401 tries again.
            log.w(t) { "refresh failed, staying retryable" }
            false
        }
    }

    private suspend fun burn(accessToken: String) {
        burnedAccessToken = accessToken
        sessionStore.setSessionInvalid(true)
    }
}

@Serializable
internal data class RefreshRequest(
    // The endpoint validates the expired token's signature to recover the
    // subject, so both halves of the pair have to be sent.
    val accessToken: String,
    val refreshToken: String,
)

/** The refresh response carries no `user` — the stored one is kept. */
@Serializable
internal data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String,
)
