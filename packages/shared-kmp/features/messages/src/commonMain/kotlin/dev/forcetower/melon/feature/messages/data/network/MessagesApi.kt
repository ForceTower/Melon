package dev.forcetower.melon.feature.messages.data.network

import dev.forcetower.melon.feature.messages.data.dto.MarkMessagesReadRequest
import dev.forcetower.melon.feature.messages.data.dto.StarMessageRequest
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

// Device acks for read/star state — the local overlay is the source of truth
// and flips first; these calls just replay the state server-side so other
// devices see it. Mirrors the ack trio in iOS `MessagesRepository+Live.swift`.
@Inject
internal class MessagesApi(private val client: HttpClient) {
    suspend fun ackRead(ids: List<String>): HttpResponse =
        client.post("api/sync/messages/read") {
            contentType(ContentType.Application.Json)
            setBody(MarkMessagesReadRequest(ids))
        }

    suspend fun ackReadAll(): HttpResponse =
        client.post("api/sync/messages/read-all")

    suspend fun ackStar(id: String, starred: Boolean): HttpResponse =
        client.post("api/sync/messages/star") {
            contentType(ContentType.Application.Json)
            setBody(StarMessageRequest(id = id, starred = starred))
        }
}
