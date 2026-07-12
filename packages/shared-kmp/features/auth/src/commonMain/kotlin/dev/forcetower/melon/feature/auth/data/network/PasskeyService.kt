package dev.forcetower.melon.feature.auth.data.network

import dev.forcetower.melon.feature.auth.data.dto.PasskeyRegisterVerifyRequest
import dev.forcetower.melon.feature.auth.data.dto.PasskeyRenameRequest
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

// Authenticated passkey-management calls. The shared client attaches the
// bearer token automatically (unlike the login-side `authenticate/*` calls,
// which run before a session exists). Rename/delete address a credential by
// id in the query string (`?id=…`), matching the API contract.
@Inject
internal class PasskeyService(private val client: HttpClient) {
    suspend fun registerOptions(): HttpResponse =
        client.post("api/passkey/register/options")

    suspend fun registerVerify(body: PasskeyRegisterVerifyRequest): HttpResponse =
        client.post("api/passkey/register/verify") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    suspend fun credentials(): HttpResponse =
        client.get("api/passkey/credentials")

    suspend fun rename(id: String, body: PasskeyRenameRequest): HttpResponse =
        client.patch("api/passkey/credentials") {
            parameter("id", id)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    suspend fun delete(id: String): HttpResponse =
        client.delete("api/passkey/credentials") {
            parameter("id", id)
        }
}
