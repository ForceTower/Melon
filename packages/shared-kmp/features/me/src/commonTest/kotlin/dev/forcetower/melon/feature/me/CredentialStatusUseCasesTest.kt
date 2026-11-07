package dev.forcetower.melon.feature.me

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.session.domain.model.AuthState
import dev.forcetower.melon.core.session.domain.model.User
import dev.forcetower.melon.core.session.domain.model.UserCredentials
import dev.forcetower.melon.feature.me.data.network.CredentialStatusService
import dev.forcetower.melon.feature.me.domain.usecase.ReauthenticateUpstreamUseCase
import dev.forcetower.melon.feature.me.domain.usecase.RefreshCredentialStatusUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class CredentialStatusUseCasesTest {
    @Test
    fun anAcceptedReauthStoresTheNewPassword() =
        runTest {
            val store = FakeSessionStore(UserCredentials("alice", "old", "2026-01-01T00:00:00Z"))
            store.setCredentialsInvalid(true)
            val reauth = ReauthenticateUpstreamUseCase(service(HttpStatusCode.OK, """{"ok":true}"""), store, Logger)

            assertEquals(Outcome.Ok(Unit), reauth("new"))

            assertEquals("alice", store.getCredentials()?.username)
            assertEquals("new", store.getCredentials()?.password)
            assertFalse(store.credentialsInvalid.value)
        }

    @Test
    fun anAcceptedReauthWithoutAStoredRowUsesThePortalUsername() =
        runTest {
            val store = FakeSessionStore(null)
            store.setUpstreamUsername("alice")
            val reauth = ReauthenticateUpstreamUseCase(service(HttpStatusCode.OK, """{"ok":true}"""), store, Logger)

            reauth("new")

            assertEquals("alice", store.getCredentials()?.username)
            assertEquals("new", store.getCredentials()?.password)
        }

    @Test
    fun aRejectedReauthKeepsTheStoredPassword() =
        runTest {
            val store = FakeSessionStore(UserCredentials("alice", "old", "2026-01-01T00:00:00Z"))
            store.setCredentialsInvalid(true)
            val reauth = ReauthenticateUpstreamUseCase(
                service(HttpStatusCode.BadRequest, """{"ok":false,"message":"Invalid"}"""),
                store,
                Logger,
            )

            reauth("wrong")

            assertEquals("old", store.getCredentials()?.password)
            assertTrue(store.credentialsInvalid.value)
        }

    @Test
    fun anUnknownStatusLeavesTheInvalidFlagAlone() =
        runTest {
            val store = FakeSessionStore(null)
            store.setCredentialsInvalid(true)
            val refresh = RefreshCredentialStatusUseCase(
                service(HttpStatusCode.OK, status("expired")),
                store,
                Logger,
            )

            refresh()

            assertTrue(store.credentialsInvalid.value)
            assertEquals("alice", store.upstreamUsername.value)
        }

    @Test
    fun knownStatusesDriveTheInvalidFlag() =
        runTest {
            val store = FakeSessionStore(null)
            RefreshCredentialStatusUseCase(service(HttpStatusCode.OK, status("invalid")), store, Logger)()
            assertTrue(store.credentialsInvalid.value)

            RefreshCredentialStatusUseCase(service(HttpStatusCode.OK, status("none")), store, Logger)()
            assertTrue(store.credentialsInvalid.value)

            RefreshCredentialStatusUseCase(service(HttpStatusCode.OK, status("ok")), store, Logger)()
            assertFalse(store.credentialsInvalid.value)
        }

    private fun status(value: String) = """{"ok":true,"data":{"credentials":{"status":"$value","username":"alice"}}}"""

    private fun service(
        status: HttpStatusCode,
        body: String,
    ): CredentialStatusService {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(body),
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return CredentialStatusService(client)
    }
}

private class FakeSessionStore(
    credentials: UserCredentials?,
) : SessionStore {
    private val credentials = MutableStateFlow(credentials)

    override val authState: StateFlow<AuthState> = MutableStateFlow(AuthState.Unauthenticated)

    private val sessionInvalidState = MutableStateFlow(false)
    override val sessionInvalid: StateFlow<Boolean> = sessionInvalidState

    private val credentialsInvalidState = MutableStateFlow(false)
    override val credentialsInvalid: StateFlow<Boolean> = credentialsInvalidState

    private val upstreamUsernameState = MutableStateFlow<String?>(null)
    override val upstreamUsername: StateFlow<String?> = upstreamUsernameState

    override suspend fun getAccessToken(): String? = "access"

    override suspend fun getRefreshToken(): String? = "refresh"

    override suspend fun currentAuthState(): AuthState = authState.value

    override suspend fun persist(
        accessToken: String,
        refreshToken: String,
        user: User,
        username: String?,
        password: String?,
    ) = Unit

    override suspend fun replaceTokens(
        staleAccessToken: String,
        accessToken: String,
        refreshToken: String,
    ) = false

    override suspend fun setSessionInvalid(invalid: Boolean) {
        sessionInvalidState.value = invalid
    }

    override suspend fun setCredentialsInvalid(invalid: Boolean) {
        credentialsInvalidState.value = invalid
    }

    override suspend fun setUpstreamUsername(username: String) {
        upstreamUsernameState.value = username
    }

    override suspend fun getCredentials(): UserCredentials? = credentials.value

    override fun observeCredentials(): Flow<UserCredentials?> = credentials

    override suspend fun updateUpstreamCredentials(
        username: String,
        password: String,
    ) {
        credentials.value = UserCredentials(username, password, "2026-11-06T00:00:00Z")
    }

    override suspend fun logout() = Unit
}
