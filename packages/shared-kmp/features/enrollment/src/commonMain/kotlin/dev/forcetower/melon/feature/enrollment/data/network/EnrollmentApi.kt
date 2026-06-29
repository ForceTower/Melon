package dev.forcetower.melon.feature.enrollment.data.network

import dev.forcetower.melon.feature.enrollment.data.dto.SubmitEnrollmentRequest
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

// The bearer token is attached by the shared client's AuthInterceptor, so these
// need no auth handling. Paths are relative to the configured base URL.
@Inject
internal class EnrollmentApi(private val client: HttpClient) {
    suspend fun window(): HttpResponse = client.get("api/enrollment/window")

    suspend fun offers(): HttpResponse = client.get("api/enrollment/offers")

    suspend fun submit(body: SubmitEnrollmentRequest): HttpResponse =
        client.post("api/enrollment/submit") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
}
