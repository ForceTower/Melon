package dev.forcetower.melon.feature.messages

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.database.MelonDatabase
import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.core.database.entity.MessageEntity
import dev.forcetower.melon.feature.messages.data.network.MessagesApi
import dev.forcetower.melon.feature.messages.domain.usecase.ObserveMessageDetailUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.ObserveMessagesInboxUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.ToggleMessageStarUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// A star change the server has not acknowledged wins over the mirrored value;
// once acknowledged, the server's merged flag alone decides — so another
// device's change shows up here too.
internal class MessageStarTest {

    @Test
    fun unstarWhoseAckFailedSurvivesTheNextInboxSync() =
        starTest(ackStatus = HttpStatusCode.ServiceUnavailable) { dao, toggleStar ->
            dao.applyServerPage(starred = true)

            toggleStar(MESSAGE_ID)
            dao.applyServerPage(starred = true)

            assertFalse(ObserveMessagesInboxUseCase(dao)().first().single().isStarred)
            assertEquals(false, ObserveMessageDetailUseCase(dao)(MESSAGE_ID).first()?.isStarred)
        }

    @Test
    fun acknowledgedStarYieldsToAnUnstarFromAnotherDevice() =
        starTest(ackStatus = HttpStatusCode.OK) { dao, toggleStar ->
            dao.applyServerPage(starred = false)

            toggleStar(MESSAGE_ID)
            assertTrue(ObserveMessagesInboxUseCase(dao)().first().single().isStarred)
            dao.applyServerPage(starred = false)

            assertFalse(ObserveMessagesInboxUseCase(dao)().first().single().isStarred)
            assertEquals(false, ObserveMessageDetailUseCase(dao)(MESSAGE_ID).first()?.isStarred)
        }

    private fun starTest(
        ackStatus: HttpStatusCode,
        test: suspend (MessageDao, ToggleMessageStarUseCase) -> Unit,
    ) = runBlocking {
        val database = Room.inMemoryDatabaseBuilder<MelonDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        val client = HttpClient(MockEngine { respond(content = "", status = ackStatus) }) {
            expectSuccess = false
            install(ContentNegotiation) { json() }
            defaultRequest { url("https://melon.test/") }
        }
        try {
            test(database.messageDao(), ToggleMessageStarUseCase(database.messageDao(), MessagesApi(client), Logger))
        } finally {
            client.close()
            database.close()
        }
    }

    private suspend fun MessageDao.applyServerPage(starred: Boolean) =
        applyMessagePage(
            messages = listOf(
                MessageEntity(
                    id = MESSAGE_ID,
                    platformId = null,
                    source = "app",
                    subject = "Aviso",
                    content = "Conteúdo",
                    senderName = "Coordenação",
                    senderType = null,
                    timestamp = "2026-10-02T12:30:00Z",
                    createdAt = "2026-10-02T12:30:00Z",
                    read = true,
                    starred = starred,
                ),
            ),
            scopes = emptyList(),
            attachments = emptyList(),
        )

    private companion object {
        const val MESSAGE_ID = "message-1"
    }
}
