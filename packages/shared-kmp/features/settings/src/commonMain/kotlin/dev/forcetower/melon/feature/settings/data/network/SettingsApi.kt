package dev.forcetower.melon.feature.settings.data.network

import dev.forcetower.melon.feature.settings.data.dto.UpdateUserSettingsRequest
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

@Inject
internal class SettingsApi(private val client: HttpClient) {
    suspend fun update(body: UpdateUserSettingsRequest): HttpResponse =
        client.patch("api/me/settings") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
}
