package dev.forcetower.melon.core.sync.data.network

import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

@Inject
internal class MirrorApi(private val client: HttpClient) {
    suspend fun getProfile(): HttpResponse = client.get("api/sync/profile")

    suspend fun getSemesters(): HttpResponse = client.get("api/sync/semesters")

    suspend fun getSemesterPayload(semesterId: String): HttpResponse =
        client.get("api/sync/semesters/$semesterId")
}
