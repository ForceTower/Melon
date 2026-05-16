package dev.forcetower.melon.feature.auth.data.network

import dev.forcetower.melon.feature.auth.data.dto.LoginRequest
import dev.forcetower.melon.feature.auth.data.dto.PasskeyAuthOptionsRequest
import dev.forcetower.melon.feature.auth.data.dto.PasskeyAuthVerifyRequest
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

@Inject
internal class AuthService(private val client: HttpClient) {
    suspend fun login(body: LoginRequest): HttpResponse =
        client.post("api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    suspend fun passkeyAuthOptions(body: PasskeyAuthOptionsRequest): HttpResponse =
        client.post("api/passkey/authenticate/options") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    suspend fun passkeyAuthVerify(body: PasskeyAuthVerifyRequest): HttpResponse =
        client.post("api/passkey/authenticate/verify") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
}
