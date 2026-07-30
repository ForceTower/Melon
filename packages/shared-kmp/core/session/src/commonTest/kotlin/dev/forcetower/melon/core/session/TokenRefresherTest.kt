package dev.forcetower.melon.core.session

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.network.BaseUrl
import dev.forcetower.melon.core.session.data.TokenRefresherImpl
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.session.domain.model.AuthState
import dev.forcetower.melon.core.session.domain.model.User
import dev.forcetower.melon.core.session.domain.model.UserCredentials
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val ROTATED =
    """{"ok":true,"message":"Token refreshed","data":{"accessToken":"rotated","refreshToken":"next"}}"""

class TokenRefresherTest {

    @Test
    fun rotatesThePairAndClearsTheInvalidFlag() = runTest {
        val store = FakeSessionStore(access = "expired", refresh = "rotatable")
        store.setSessionInvalid(true)
        var calls = 0
        val refresher = refresher(store) {
            calls++
            HttpStatusCode.OK to ROTATED
        }

        assertTrue(refresher.refresh("expired"))

        assertEquals(1, calls)
        assertEquals("rotated", store.getAccessToken())
        assertEquals("next", store.getRefreshToken())
        assertFalse(store.sessionInvalid.value)
    }

    @Test
    fun concurrentCallersRotateOnlyOnce() = runTest {
        val store = FakeSessionStore(access = "expired", refresh = "rotatable")
        var calls = 0
        val refresher = refresher(store) {
            calls++
            // Hold the first caller in flight so the others pile onto the mutex.
            delay(50)
            HttpStatusCode.OK to ROTATED
        }

        val results = (0 until 8).map { async { refresher.refresh("expired") } }.awaitAll()

        // The refresh token is burned on first use, so a second rotation would
        // strand the session.
        assertEquals(1, calls)
        assertTrue(results.all { it })
        assertEquals("rotated", store.getAccessToken())
    }

    @Test
    fun aRejectedRefreshFlagsTheSessionAndIsNotRetried() = runTest {
        val store = FakeSessionStore(access = "expired", refresh = "spent")
        var calls = 0
        val refresher = refresher(store) {
            calls++
            HttpStatusCode.BadRequest to """{"ok":false,"message":"Invalid refresh token"}"""
        }

        assertFalse(refresher.refresh("expired"))
        // The latch keeps a structurally dead session from re-attempting the
        // refresh on every subsequent request.
        assertFalse(refresher.refresh("expired"))

        assertEquals(1, calls)
        assertTrue(store.sessionInvalid.value)
        // The session is kept so the re-auth sheet can swap tokens in place.
        assertEquals("expired", store.getAccessToken())
    }

    @Test
    fun aServerErrorStaysRetryable() = runTest {
        val store = FakeSessionStore(access = "expired", refresh = "rotatable")
        var calls = 0
        val refresher = refresher(store) {
            calls++
            HttpStatusCode.ServiceUnavailable to """{"ok":false,"message":"Service Unavailable"}"""
        }

        assertFalse(refresher.refresh("expired"))
        assertFalse(refresher.refresh("expired"))

        // A 5xx is the server's problem — the pair is untouched, so the next
        // 401 tries again and nothing is flagged.
        assertEquals(2, calls)
        assertFalse(store.sessionInvalid.value)
    }

    @Test
    fun anAlreadyRotatedTokenSkipsTheCallAndRetries() = runTest {
        val store = FakeSessionStore(access = "rotated", refresh = "next")
        var calls = 0
        val refresher = refresher(store) {
            calls++
            HttpStatusCode.OK to ROTATED
        }

        // Another caller won the race while this request sat on its 401.
        assertTrue(refresher.refresh("expired"))

        assertEquals(0, calls)
    }

    @Test
    fun anEmptyRefreshTokenIsTerminalWithoutACall() = runTest {
        // Sessions adopted from the legacy app can carry an empty refresh token.
        val store = FakeSessionStore(access = "expired", refresh = "")
        var calls = 0
        val refresher = refresher(store) {
            calls++
            HttpStatusCode.OK to ROTATED
        }

        assertFalse(refresher.refresh("expired"))

        assertEquals(0, calls)
        assertTrue(store.sessionInvalid.value)
    }

    private fun refresher(
        store: SessionStore,
        respondWith: suspend () -> Pair<HttpStatusCode, String>,
    ) = TokenRefresherImpl(
        sessionStore = store,
        engine = MockEngine {
            val (status, body) = respondWith()
            respond(
                content = ByteReadChannel(body),
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
        baseUrl = BaseUrl("https://melon.test/"),
        json = Json { ignoreUnknownKeys = true },
        logger = Logger,
    )
}

private class FakeSessionStore(
    private var access: String?,
    private var refresh: String?,
) : SessionStore {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState

    private val _sessionInvalid = MutableStateFlow(false)
    override val sessionInvalid: StateFlow<Boolean> = _sessionInvalid

    override suspend fun getAccessToken(): String? = access

    override suspend fun getRefreshToken(): String? = refresh

    override suspend fun replaceTokens(accessToken: String, refreshToken: String) {
        access = accessToken
        refresh = refreshToken
    }

    override suspend fun setSessionInvalid(invalid: Boolean) {
        _sessionInvalid.value = invalid
    }

    override suspend fun currentAuthState(): AuthState = _authState.value

    override suspend fun persist(
        accessToken: String,
        refreshToken: String,
        user: User,
        username: String?,
        password: String?,
    ) {
        access = accessToken
        refresh = refreshToken
        _authState.value = AuthState.Authenticated(user)
    }

    override suspend fun getCredentials(): UserCredentials? = null

    override fun observeCredentials(): Flow<UserCredentials?> = flowOf(null)

    override suspend fun updateUpstreamCredentials(username: String, password: String) = Unit

    override suspend fun logout() {
        access = null
        refresh = null
        _authState.value = AuthState.Unauthenticated
    }
}
