package dev.forcetower.melon.feature.settings

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.database.MelonDatabase
import dev.forcetower.melon.core.database.dao.UserSettingsDao
import dev.forcetower.melon.core.database.entity.UserSettingsEntity
import dev.forcetower.melon.feature.settings.data.network.SettingsApi
import dev.forcetower.melon.feature.settings.data.repository.SettingsRepositoryImpl
import dev.forcetower.melon.feature.settings.domain.model.UserSettingsPatch
import dev.forcetower.melon.feature.settings.domain.repository.SettingsError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

// The local row may only keep a settings change the server confirmed: the
// next profile sync overwrites the row with the server's values, so an
// unconfirmed change would silently revert later while the server keeps
// notifying by the old preference.
internal class SettingsRepositoryTest {

    @Test
    fun rejectedChangeIsRolledBackAndReported() =
        settingsTest(respond = {
            respond(content = "", status = HttpStatusCode.ServiceUnavailable)
        }) { dao, repository ->
            val outcome = repository.update(UserSettingsPatch(notifGradePosted = false))

            assertEquals(Outcome.Err(SettingsError.Server(null)), outcome)
            assertEquals(true, dao.getCurrent()?.notifGradePosted)
        }

    @Test
    fun changeThatNeverReachedTheServerIsRolledBack() =
        settingsTest(respond = { throw IOException("offline") }) { dao, repository ->
            val outcome = repository.update(UserSettingsPatch(notifGradePosted = false, gradeSpoiler = 2))

            assertEquals(Outcome.Err(SettingsError.NoConnection), outcome)
            assertEquals(seeded, dao.getCurrent())
        }

    @Test
    fun confirmedChangeKeepsTheServerRow() =
        settingsTest(respond = { respondSettings(seeded.copy(notifGradePosted = false)) }) { dao, repository ->
            val outcome = repository.update(UserSettingsPatch(notifGradePosted = false))

            assertIs<Outcome.Ok<*>>(outcome)
            assertEquals(seeded.copy(notifGradePosted = false), dao.getCurrent())
        }

    @Test
    fun rollbackKeepsAValueALaterConfirmedChangeWrote() {
        val firstArrived = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        settingsTest(
            respond = { request ->
                if (request.body.toByteArray().decodeToString().contains("\"gradeSpoiler\":1")) {
                    firstArrived.complete(Unit)
                    releaseFirst.await()
                    respond(content = "", status = HttpStatusCode.ServiceUnavailable)
                } else {
                    respondSettings(seeded.copy(gradeSpoiler = 2))
                }
            },
        ) { dao, repository ->
            val first = async(Dispatchers.IO) { repository.update(UserSettingsPatch(gradeSpoiler = 1)) }
            firstArrived.await()
            assertIs<Outcome.Ok<*>>(repository.update(UserSettingsPatch(gradeSpoiler = 2)))
            releaseFirst.complete(Unit)

            assertIs<Outcome.Err<*>>(first.await())
            assertEquals(2, dao.getCurrent()?.gradeSpoiler)
        }
    }

    private val seeded = UserSettingsEntity(
        userId = "user-1",
        gradeSpoiler = 0,
        notifMsgBroadcast = true,
        notifMsgClass = true,
        notifMsgDirect = true,
        notifGradePosted = true,
        notifGradeChanged = true,
        notifGradeDateChanged = true,
        notifClassLocation = true,
        notifClassMaterial = true,
        notifClassSubject = true,
    )

    private fun settingsTest(
        respond: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
        test: suspend CoroutineScope.(UserSettingsDao, SettingsRepositoryImpl) -> Unit,
    ) = runBlocking {
        val database = Room.inMemoryDatabaseBuilder<MelonDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        val client = HttpClient(MockEngine { request -> respond(request) }) {
            expectSuccess = false
            install(ContentNegotiation) { json() }
            defaultRequest { url("https://melon.test/") }
        }
        try {
            val dao = database.userSettingsDao()
            dao.upsert(seeded)
            test(dao, SettingsRepositoryImpl(dao, SettingsApi(client), Logger))
        } finally {
            client.close()
            database.close()
        }
    }

    private fun MockRequestHandleScope.respondSettings(settings: UserSettingsEntity): HttpResponseData =
        respond(
            content = """
                {"ok":true,"data":{"settings":{
                  "gradeSpoiler":${settings.gradeSpoiler},
                  "notifMsgBroadcast":${settings.notifMsgBroadcast},
                  "notifMsgClass":${settings.notifMsgClass},
                  "notifMsgDirect":${settings.notifMsgDirect},
                  "notifGradePosted":${settings.notifGradePosted},
                  "notifGradeChanged":${settings.notifGradeChanged},
                  "notifGradeDateChanged":${settings.notifGradeDateChanged},
                  "notifClassLocation":${settings.notifClassLocation},
                  "notifClassMaterial":${settings.notifClassMaterial},
                  "notifClassSubject":${settings.notifClassSubject}
                }}}
            """.trimIndent(),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
}
