package dev.forcetower.melon.feature.me.data.network

import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Profile customization endpoints. Name and picture are separate calls on the
// API (`PATCH api/me/name`, `POST/DELETE api/me/picture`); the caller re-pulls
// `api/sync/profile` afterwards so the mirrored User row picks up whatever the
// server normalized (e.g. re-typing the official name stores null).
@Inject
internal class ProfileService(private val client: HttpClient) {

    // The `name` key is required but nullable server-side: null (or blank,
    // which the API also nulls) clears the alternate name, while a missing
    // key is a 400. The shared Json is configured with `explicitNulls =
    // false`, which would drop the key from a @Serializable body — build the
    // object manually so `{"name":null}` actually goes over the wire.
    suspend fun updateName(name: String?): HttpResponse =
        client.patch("api/me/name") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("name", name) })
        }

    // Multipart with a single `file` part. The part's Content-Type must be
    // the real image MIME — the server sniffs magic numbers against it and
    // rejects a mismatch (or a generic octet-stream) with a 400.
    suspend fun uploadPicture(bytes: ByteArray, mimeType: String): HttpResponse =
        client.post("api/me/picture") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"avatar\"")
                            },
                        )
                    },
                ),
            )
        }

    suspend fun deletePicture(): HttpResponse = client.delete("api/me/picture")
}
