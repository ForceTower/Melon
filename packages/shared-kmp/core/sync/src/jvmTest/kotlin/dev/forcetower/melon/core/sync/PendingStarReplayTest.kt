package dev.forcetower.melon.core.sync

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.MessagePageResult
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray

// A star change whose device ack never landed (offline, 5xx) must still reach
// the server: the next inbox sync replays it, and only an acknowledged change
// hands display back to the server value.
internal class PendingStarReplayTest {
    private val message = contractFixture("/v1/messages.json")
        .getValue("page").jsonObject
        .getValue("messages").jsonArray.single().jsonObject
    private val messageId = message.getValue("id").jsonPrimitive.content

    @Test
    fun firstPageSyncReplaysAnUnacknowledgedUnstarBeforePulling() {
        var serverStarred = true
        val replayed = mutableListOf<Boolean>()
        mirrorTest(
            respond = { request, _ ->
                when (request.url.encodedPath) {
                    "/api/sync/messages/star" -> {
                        val body = testJson.parseToJsonElement(request.body.toByteArray().decodeToString())
                        serverStarred = body.jsonObject.getValue("starred").jsonPrimitive.boolean
                        replayed += serverStarred
                        respondEnvelope("{}")
                    }
                    "/api/sync/messages" -> respondEnvelope(page(serverStarred))
                    else -> error("Unexpected request ${request.url}")
                }
            },
        ) { database, repository ->
            val dao = database.messageDao()
            repository.syncMessages(since = null, cursor = null)
            dao.markStarPending(messageId, starred = false, now = "2026-10-02T13:00:00Z")

            assertIs<Outcome.Ok<MessagePageResult>>(repository.syncMessages(since = null, cursor = null))

            assertEquals(listOf(false), replayed)
            assertEquals(emptyList(), dao.listPendingStars())
            assertEquals(false, dao.getMessage(messageId)?.starred)
        }
    }

    @Test
    fun rejectedReplayStaysPendingAndThePageStillLands() =
        mirrorTest(
            respond = { request, _ ->
                when (request.url.encodedPath) {
                    "/api/sync/messages/star" -> respond(content = "", status = HttpStatusCode.ServiceUnavailable)
                    "/api/sync/messages" -> respondEnvelope(page(starred = true))
                    else -> error("Unexpected request ${request.url}")
                }
            },
        ) { database, repository ->
            val dao = database.messageDao()
            repository.syncMessages(since = null, cursor = null)
            dao.markStarPending(messageId, starred = false, now = "2026-10-02T13:00:00Z")

            assertIs<Outcome.Ok<MessagePageResult>>(repository.syncMessages(since = null, cursor = null))

            assertEquals(listOf(messageId), dao.listPendingStars().map { it.messageId })
            assertEquals(true, dao.getMessage(messageId)?.starred)
        }

    private fun page(starred: Boolean): String =
        buildJsonObject {
            putJsonArray("messages") { add(JsonObject(message + ("starred" to JsonPrimitive(starred)))) }
            put("nextCursor", JsonNull)
        }.toString()
}
