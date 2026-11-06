package dev.forcetower.melon.feature.campusevent

import dev.forcetower.melon.core.storage.KeyValueStorage
import dev.forcetower.melon.feature.campusevent.data.CampusEventRepository
import dev.forcetower.melon.feature.campusevent.data.network.CampusEventService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class CampusEventRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun anAnswerLandingAfterLogoutIsDropped() =
        runTest {
            val storage = MemoryStorage()
            val answer = CompletableDeferred<String>()
            val repository = repository(storage, listOf(answer))

            launch { repository.refresh() }
            runCurrent()
            repository.clear()
            answer.complete(feed(revision = 1))
            advanceUntilIdle()

            assertNull(repository.observe().first())
            assertNull(storage.get(STORAGE_KEY))
        }

    @Test
    fun anOlderAnswerNeverOverwritesANewerOne() =
        runTest {
            val older = CompletableDeferred<String>()
            val newer = CompletableDeferred<String>()
            val repository = repository(MemoryStorage(), listOf(older, newer))

            launch { repository.refresh() }
            runCurrent()
            launch { repository.refresh() }
            runCurrent()
            newer.complete(feed(revision = 2))
            runCurrent()
            older.complete(feed(revision = 1))
            advanceUntilIdle()

            assertEquals(2, repository.observe().first()?.revision)
        }

    private fun TestScope.repository(
        storage: KeyValueStorage,
        answers: List<CompletableDeferred<String>>,
    ): CampusEventRepository {
        val config = MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            answers.forEach { answer ->
                addHandler {
                    respond(
                        content = ByteReadChannel(answer.await()),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }
        val client = HttpClient(MockEngine(config)) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
        }
        return CampusEventRepository(CampusEventService(client), storage, json)
    }

    private fun feed(revision: Int) =
        """
        {"ok":true,"data":{"event":{
          "id":"week","revision":$revision,"name":"Semana","startsAt":"2026-11-10T12:00:00Z",
          "endsAt":"2026-11-12T21:00:00Z","activities":[]
        }}}
        """.trimIndent()

    private companion object {
        const val STORAGE_KEY = "campus_event.current"
    }
}

private class MemoryStorage : KeyValueStorage {
    private val values = mutableMapOf<String, String>()

    override suspend fun get(key: String): String? = values[key]

    override suspend fun put(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override suspend fun remove(key: String) {
        values.remove(key)
    }
}
