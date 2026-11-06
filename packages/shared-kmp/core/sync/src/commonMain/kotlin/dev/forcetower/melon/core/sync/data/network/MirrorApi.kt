package dev.forcetower.melon.core.sync.data.network

import dev.forcetower.melon.core.sync.data.dto.StarMessageRequest
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

@Inject
internal class MirrorApi(
    private val client: HttpClient,
) {
    suspend fun getProfile(): HttpResponse = client.get("api/sync/profile")

    suspend fun getSemesters(): HttpResponse = client.get("api/sync/semesters")

    suspend fun getSemesterPayload(semesterId: String): HttpResponse = client.get("api/sync/semesters/$semesterId")

    suspend fun getOnboardingStatus(): HttpResponse = client.get("api/sync/onboarding-status")

    suspend fun getMessages(
        since: String?,
        cursor: String?,
    ): HttpResponse =
        client.get("api/sync/messages") {
            since?.let { parameter("since", it) }
            cursor?.let { parameter("cursor", it) }
        }

    suspend fun ackStar(
        id: String,
        starred: Boolean,
    ): HttpResponse =
        client.post("api/sync/messages/star") {
            contentType(ContentType.Application.Json)
            setBody(StarMessageRequest(id = id, starred = starred))
        }

    suspend fun getCalendarEvents(): HttpResponse = client.get("api/sync/events")

    // Bumps users.last_active_at server-side. Co-located here for now to avoid
    // a separate API surface for one tiny fire-and-forget endpoint.
    suspend fun ping(): HttpResponse = client.post("api/me/ping")

    // Returns the cached upstream Snowpiercer credentials for the current
    // user — used during initial mirror sync to backfill the local Credentials
    // row for clients that signed in via passkey.
    suspend fun getMyCredentials(): HttpResponse = client.get("api/me/credentials")
}
