package dev.forcetower.melon.core.sync

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.database.MelonDatabase
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.sync.data.network.MirrorApi
import dev.forcetower.melon.core.sync.data.repository.MirrorRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

internal val testJson = Json { ignoreUnknownKeys = true }

internal fun contractFixture(path: String): JsonObject =
    requireNotNull(MirrorRepositoryImpl::class.java.getResourceAsStream(path)).bufferedReader().use {
        testJson.parseToJsonElement(it.readText()).jsonObject
    }

// A real MirrorRepositoryImpl over an in-memory Room mirror and a mock server.
// The handler gets the repository so it can interleave a second sync with
// the request it is answering.
internal fun mirrorTest(
    respond: suspend MockRequestHandleScope.(HttpRequestData, MirrorRepositoryImpl) -> HttpResponseData,
    test: suspend (MelonDatabase, MirrorRepositoryImpl) -> Unit,
) = runBlocking {
    val database = Room.inMemoryDatabaseBuilder<MelonDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    lateinit var repository: MirrorRepositoryImpl
    val client = HttpClient(MockEngine { request -> respond(request, repository) }) {
        expectSuccess = false
        install(ContentNegotiation) { json(testJson) }
        defaultRequest { url("https://melon.test/") }
    }
    repository = MirrorRepositoryImpl(
        api = MirrorApi(client),
        userDao = database.userDao(),
        studentDao = database.studentDao(),
        semesterDao = database.semesterDao(),
        academicDao = database.academicDao(),
        messageDao = database.messageDao(),
        calendarEventDao = database.calendarEventDao(),
        userSettingsDao = database.userSettingsDao(),
        sessionStore = unusedSessionStore(),
        logger = Logger,
    )
    try {
        test(database, repository)
    } finally {
        client.close()
        database.close()
    }
}

internal fun MockRequestHandleScope.respondEnvelope(data: String): HttpResponseData =
    respond(
        content = """{"ok":true,"data":$data}""",
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

private fun unusedSessionStore(): SessionStore =
    Proxy.newProxyInstance(SessionStore::class.java.classLoader, arrayOf(SessionStore::class.java)) { _, method, _ ->
        error("SessionStore.${method.name} is not used by these tests")
    } as SessionStore
