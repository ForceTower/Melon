package dev.forcetower.melon.feature.notifications.data.network

import dev.forcetower.melon.feature.notifications.data.dto.RegisterNotificationTokenRequest
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

@Inject
internal class NotificationTokenService(private val client: HttpClient) {
    suspend fun registerToken(body: RegisterNotificationTokenRequest): HttpResponse =
        client.post("api/notifications/token") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
}
