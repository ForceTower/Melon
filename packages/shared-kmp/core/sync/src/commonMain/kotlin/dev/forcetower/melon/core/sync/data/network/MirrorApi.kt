package dev.forcetower.melon.core.sync.data.network

import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse

@Inject
internal class MirrorApi(private val client: HttpClient) {
    suspend fun getProfile(): HttpResponse = client.get("api/sync/profile")

    suspend fun getSemesters(): HttpResponse = client.get("api/sync/semesters")

    suspend fun getSemesterPayload(semesterId: String): HttpResponse =
        client.get("api/sync/semesters/$semesterId")

    suspend fun getOnboardingStatus(): HttpResponse = client.get("api/sync/onboarding-status")

    suspend fun getMessages(since: String?, cursor: String?): HttpResponse =
        client.get("api/sync/messages") {
            since?.let { parameter("since", it) }
            cursor?.let { parameter("cursor", it) }
        }

    // Bumps users.last_active_at server-side. Co-located here for now to avoid
    // a separate API surface for one tiny fire-and-forget endpoint.
    suspend fun ping(): HttpResponse = client.post("api/me/ping")
}
