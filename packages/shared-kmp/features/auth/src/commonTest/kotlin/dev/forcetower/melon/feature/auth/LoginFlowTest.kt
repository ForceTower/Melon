package dev.forcetower.melon.feature.auth

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.AuthTokenSource
import dev.forcetower.melon.core.session.domain.model.AuthState
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.session.domain.model.User
import dev.forcetower.melon.core.session.domain.model.UserCredentials
import dev.forcetower.melon.feature.auth.data.network.AuthService
import dev.forcetower.melon.feature.auth.data.repository.AuthRepositoryImpl
import dev.forcetower.melon.feature.auth.domain.model.LoginError
import dev.forcetower.melon.feature.auth.domain.usecase.LoginUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class LoginFlowTest {

    @Test
    fun login_success_persists_session_and_emits_authenticated() = runTest {
        val sessionStore = RecordingSessionStore()
        val useCase = buildUseCase(sessionStore) { _ ->
            jsonResponse(
                """
                {
                  "ok": true,
                  "message": "Login successful",
                  "data": {
                    "accessToken": "access-123",
                    "refreshToken": "refresh-456",
                    "user": { "id": "user-1", "name": "Alice", "imageUrl": null }
                  },
                  "error": null
                }
                """.trimIndent(),
                HttpStatusCode.OK,
            )
        }

        val result = useCase("alice", "hunter2")

        assertIs<Outcome.Ok<Unit>>(result)
        assertEquals("access-123", sessionStore.lastAccessToken)
        assertEquals("refresh-456", sessionStore.lastRefreshToken)
        assertEquals(User("user-1", "Alice", null), sessionStore.lastUser)
        assertEquals("alice", sessionStore.lastUsername)
        assertEquals("hunter2", sessionStore.lastPassword)
        assertEquals(
            AuthState.Authenticated(User("user-1", "Alice", null)),
            sessionStore.authState.value,
        )
    }

    @Test
    fun login_invalid_credentials_maps_to_domain_error() = runTest {
        val sessionStore = RecordingSessionStore()
        val useCase = buildUseCase(sessionStore) { _ ->
            jsonResponse(
                """{"ok":false,"message":"Invalid credentials","data":null,"error":null}""",
                HttpStatusCode.BadRequest,
            )
        }

        val result = useCase("alice", "wrong")

        assertEquals(Outcome.Err(LoginError.Kind.InvalidCredentials), result)
        assertNull(sessionStore.lastAccessToken)
    }

    private fun buildUseCase(
        sessionStore: SessionStore,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): LoginUseCase {
        val client = HttpClient(MockEngine(handler)) {
            expectSuccess = false
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return LoginUseCase(AuthRepositoryImpl(AuthService(client), sessionStore, Logger))
    }

    private fun MockRequestHandleScope.jsonResponse(body: String, status: HttpStatusCode): HttpResponseData =
        respond(
            content = ByteReadChannel(body),
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
}

private class RecordingSessionStore : SessionStore, AuthTokenSource {
    var lastAccessToken: String? = null
    var lastRefreshToken: String? = null
    var lastUser: User? = null
    var lastUsername: String? = null
    var lastPassword: String? = null
    var loggedOut: Boolean = false

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState

    override suspend fun getAccessToken(): String? = lastAccessToken

    override suspend fun persist(
        accessToken: String,
        refreshToken: String,
        user: User,
        username: String?,
        password: String?,
    ) {
        lastAccessToken = accessToken
        lastRefreshToken = refreshToken
        lastUser = user
        lastUsername = username
        lastPassword = password
        _authState.value = AuthState.Authenticated(user)
    }

    override suspend fun getCredentials(): UserCredentials? = null

    override fun observeCredentials(): Flow<UserCredentials?> = flowOf(null)

    override suspend fun updateUpstreamCredentials(username: String, password: String) {
        lastUsername = username
        lastPassword = password
    }

    override suspend fun logout() {
        lastAccessToken = null
        lastRefreshToken = null
        lastUser = null
        lastUsername = null
        lastPassword = null
        loggedOut = true
        _authState.value = AuthState.Unauthenticated
    }
}
