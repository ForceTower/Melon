package dev.forcetower.melon.feature.auth.data.network

import dev.forcetower.melon.feature.auth.data.dto.LoginRequest
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
}
