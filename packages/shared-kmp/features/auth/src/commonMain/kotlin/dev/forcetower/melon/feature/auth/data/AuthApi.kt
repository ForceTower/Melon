package dev.forcetower.melon.feature.auth.data

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class AuthApi(private val client: HttpClient) {
    suspend fun login(body: LoginRequest): HttpResponse =
        client.post("api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
}
