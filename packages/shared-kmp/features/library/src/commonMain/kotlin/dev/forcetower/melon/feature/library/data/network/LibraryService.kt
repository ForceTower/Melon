package dev.forcetower.melon.feature.library.data.network

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.feature.library.data.dto.LibraryAvailabilityBody
import dev.forcetower.melon.feature.library.data.dto.LibraryOverviewBody
import dev.forcetower.melon.feature.library.data.dto.LibrarySearchBody
import dev.forcetower.melon.feature.library.domain.model.LibraryAvailabilitySnapshot
import dev.forcetower.melon.feature.library.domain.model.LibraryError
import dev.forcetower.melon.feature.library.domain.model.LibraryFacetGroup
import dev.forcetower.melon.feature.library.domain.model.LibraryOverview
import dev.forcetower.melon.feature.library.domain.model.LibraryReading
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchPage
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchRequest
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

// `api/library/*` — the Pergamum catalogue. All reads are live (nothing
// mirrors into the local DB). Search answers fast from the catalogue; the
// per-work availability consultation is slower and degrades to a state, not
// an error. Mirrors iOS `LibraryRepository+Live.swift`, which defined the
// contract client-first.
@Inject
internal class LibraryService(private val client: HttpClient) {

    suspend fun overview(): Outcome<LibraryOverview, LibraryError> =
        envelope<LibraryOverviewBody> { client.get("api/library/overview") }
            .map { it.toDomain() }

    suspend fun search(request: LibrarySearchRequest): Outcome<LibrarySearchPage, LibraryError> {
        val first = request.terms.firstOrNull()
            ?: return Outcome.Ok(LibrarySearchPage(emptyList(), total = 0, offset = 0, facets = emptyMap()))
        return envelope<LibrarySearchBody> {
            client.get("api/library/search") {
                parameter("q", first.query)
                parameter("scope", first.scope.wire)
                // Terms two and three ride as q2/scope2/op2 and q3/scope3/op3
                // triplets, mirroring upstream's E / OU / NÃO.
                request.terms.drop(1).take(2).forEachIndexed { index, term ->
                    val slot = index + 2
                    parameter("q$slot", term.query)
                    parameter("scope$slot", term.scope.wire)
                    parameter("op$slot", term.op.wire)
                }
                parameter("sort", request.sort.wire)
                parameter("offset", request.offset.toString())
                parameter("limit", request.limit.toString())
                // Facet keys are free text, so each group rides as its own
                // pipe-separated parameter.
                LibraryFacetGroup.entries.forEach { group ->
                    val keys = request.facets[group].orEmpty()
                    if (keys.isNotEmpty()) {
                        parameter("f${group.wire}", keys.sorted().joinToString("|"))
                    }
                }
            }
        }.map { it.toDomain() }
    }

    // Degradation is a state, not an error — the screens narrate it, so this
    // call never fails outward.
    suspend fun availability(workId: String): LibraryAvailabilitySnapshot =
        when (val outcome = envelope<LibraryAvailabilityBody> {
            client.get("api/library/works/$workId/availability")
        }) {
            is Outcome.Ok -> outcome.value.toDomain()
            is Outcome.Err -> LibraryAvailabilitySnapshot(
                reading = LibraryReading.Unavailable,
                copies = emptyList(),
            )
        }

    suspend fun clearRecents(): Outcome<Unit, LibraryError> =
        confirm { client.delete("api/library/recents") }

    // Unwraps the `{ ok, data }` envelope; any transport/decoding hiccup or
    // `ok=false` collapses to Connection — the screens only distinguish
    // "worked" from "retry".
    private suspend inline fun <reified T> envelope(
        request: () -> HttpResponse,
    ): Outcome<T, LibraryError> {
        val response = try {
            request()
        } catch (_: Exception) {
            return Outcome.Err(LibraryError.Connection)
        }
        if (!response.status.isSuccess()) return Outcome.Err(LibraryError.Connection)
        val data = runCatching { response.body<ApiEnvelope<T>>() }
            .getOrNull()
            ?.takeIf { it.ok }
            ?.data
            ?: return Outcome.Err(LibraryError.Connection)
        return Outcome.Ok(data)
    }

    // For mutations whose envelope carries no data payload.
    private suspend inline fun confirm(request: () -> HttpResponse): Outcome<Unit, LibraryError> {
        val response = try {
            request()
        } catch (_: Exception) {
            return Outcome.Err(LibraryError.Connection)
        }
        return if (response.status.isSuccess()) Outcome.Ok(Unit)
        else Outcome.Err(LibraryError.Connection)
    }
}

private fun <T, R, E> Outcome<T, E>.map(transform: (T) -> R): Outcome<R, E> = when (this) {
    is Outcome.Ok -> Outcome.Ok(transform(value))
    is Outcome.Err -> this
}
