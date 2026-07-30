package dev.forcetower.melon.feature.me.data.network

import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

// `GET api/me/status` is the polled health signal for the SAGRES credentials
// the server syncs with; `POST api/me/credentials` replaces the password after
// it stops working. Mirrors iOS `CredentialStatusRepository`.
@Inject
internal class CredentialStatusService(private val client: HttpClient) {
    suspend fun status(): HttpResponse = client.get("api/me/status")

    suspend fun reauthenticate(body: ReauthRequest): HttpResponse =
        client.post("api/me/credentials") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
}

@Serializable
internal data class CredentialStatusResponse(val credentials: CredentialHealthDto)

@Serializable
internal data class CredentialHealthDto(
    val status: String,
    val username: String? = null,
)

@Serializable
internal data class ReauthRequest(
    val password: String,
    // Only needed once the portal's reCAPTCHA is back and the server is
    // running on the HTML source.
    val captchaToken: String? = null,
)
