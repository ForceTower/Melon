package dev.forcetower.melon.feature.enrollment

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.enrollment.data.network.EnrollmentApi
import dev.forcetower.melon.feature.enrollment.data.repository.EnrollmentRepositoryImpl
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentAvailability
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSelection
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindowState
import dev.forcetower.melon.feature.enrollment.domain.repository.EnrollmentError
import dev.forcetower.melon.feature.enrollment.domain.usecase.GetEnrollmentOffersUseCase
import dev.forcetower.melon.feature.enrollment.domain.usecase.GetEnrollmentWindowUseCase
import dev.forcetower.melon.feature.enrollment.domain.usecase.SubmitEnrollmentUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal class EnrollmentRepositoryTest {
    @Test
    fun submitSendsTheWholeDesiredSetWithTogglesAndDoesNotRetainThePreviousProposal() =
        runTest {
            val requests = mutableListOf<JsonObject>()
            withRepository({ request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/api/enrollment/submit", request.url.encodedPath)
                val body = assertIs<TextContent>(request.body)
                assertEquals("application/json", body.contentType.toString())
                requests += Json.decodeFromString<JsonObject>(body.text)
                jsonResponse("""{"ok":true,"data":{}}""")
            }) { repository ->
                val submit = SubmitEnrollmentUseCase(repository)
                assertEquals(
                    Outcome.Ok(Unit),
                    submit(listOf(EnrollmentSelection(11, false, false), EnrollmentSelection(21, true, false))),
                )
                assertEquals(
                    Outcome.Ok(Unit),
                    submit(listOf(EnrollmentSelection(12, true, true), EnrollmentSelection(31, false, false))),
                )
            }

            assertEquals(
                listOf(
                    Json.parseToJsonElement(
                        """{"selections":[
                        {"sectionId":11,"allowsOther":false,"waitlist":false},
                        {"sectionId":21,"allowsOther":true,"waitlist":false}
                    ]}""",
                    ),
                    Json.parseToJsonElement(
                        """{"selections":[
                        {"sectionId":12,"allowsOther":true,"waitlist":true},
                        {"sectionId":31,"allowsOther":false,"waitlist":false}
                    ]}""",
                    ),
                ),
                requests,
            )
        }

    @Test
    fun failedSubmissionCanRetryTheSameCompleteProposal() =
        runTest {
            val requests = mutableListOf<String>()
            withRepository({ request ->
                requests += assertIs<TextContent>(request.body).text
                if (requests.size == 1) {
                    jsonResponse(
                        """{"ok":false,"message":"Synthetic temporary outage","data":null}""",
                        HttpStatusCode.ServiceUnavailable,
                    )
                } else {
                    jsonResponse("""{"ok":true,"data":{}}""")
                }
            }) { repository ->
                val submit = SubmitEnrollmentUseCase(repository)
                val proposal = listOf(EnrollmentSelection(11, true, false), EnrollmentSelection(21, false, true))
                assertEquals(Outcome.Err(EnrollmentError.Server("Synthetic temporary outage")), submit(proposal))
                assertEquals(Outcome.Ok(Unit), submit(proposal))
            }
            assertEquals(2, requests.size)
            assertEquals(requests.first(), requests.last())
        }

    @Test
    fun unauthorizedResponsesStayDistinctForWindowOffersAndSubmission() =
        runTest {
            withRepository({
                jsonResponse("""{"ok":false,"message":"Synthetic expired session"}""", HttpStatusCode.Unauthorized)
            }) { repository ->
                val expected = Outcome.Err(EnrollmentError.Unauthorized)
                assertEquals(expected, GetEnrollmentWindowUseCase(repository)())
                assertEquals(expected, GetEnrollmentOffersUseCase(repository)())
                assertEquals(expected, SubmitEnrollmentUseCase(repository)(emptyList()))
            }
        }

    @Test
    fun serverFailuresPreserveTheirExplanationForAllEnrollmentRequests() =
        runTest {
            withRepository({
                jsonResponse(
                    """{"ok":false,"message":"Synthetic portal unavailable","data":null,"error":"UPSTREAM"}""",
                    HttpStatusCode.BadGateway,
                )
            }) { repository ->
                val expected = Outcome.Err(EnrollmentError.Server("Synthetic portal unavailable"))
                assertEquals(expected, repository.window())
                assertEquals(expected, repository.offers())
                assertEquals(expected, repository.submit(emptyList()))
            }
        }

    @Test
    fun nonJsonServerFailureStillMapsToServerError() =
        runTest {
            withRepository({
                respond("Synthetic upstream unavailable", HttpStatusCode.ServiceUnavailable)
            }) { repository ->
                assertEquals(Outcome.Err(EnrollmentError.Server(null)), repository.submit(emptyList()))
            }
        }

    @Test
    fun rejectedRequestsDoNotBecomeSuccessfulSubmissions() =
        runTest {
            withRepository({ jsonResponse("""{"ok":false}""", HttpStatusCode.BadRequest) }) { repository ->
                assertEquals(Outcome.Err(EnrollmentError.Unexpected), repository.window())
                assertEquals(Outcome.Err(EnrollmentError.Unexpected), repository.offers())
                assertEquals(Outcome.Err(EnrollmentError.Unexpected), repository.submit(emptyList()))
            }
        }

    @Test
    fun missingResponseDataDoesNotLookLikeAnUnavailableWindowOrEmptyOffers() =
        runTest {
            withRepository({ jsonResponse("""{"ok":true,"data":null}""") }) { repository ->
                assertEquals(Outcome.Err(EnrollmentError.Unexpected), repository.window())
                assertEquals(Outcome.Err(EnrollmentError.Unexpected), repository.offers())
            }
        }

    @Test
    fun transportFailureMapsToNoConnectionForEveryOperation() =
        runTest {
            withRepository({ error("Synthetic disconnected transport") }) { repository ->
                assertEquals(Outcome.Err(EnrollmentError.NoConnection), repository.window())
                assertEquals(Outcome.Err(EnrollmentError.NoConnection), repository.offers())
                assertEquals(Outcome.Err(EnrollmentError.NoConnection), repository.submit(emptyList()))
            }
        }

    @Test
    fun cancellationIsNeverTurnedIntoARetryableNetworkFailure() =
        runTest {
            for (operation in listOf("window", "offers", "submit")) {
                withRepository({ throw CancellationException("Synthetic cancellation") }) { repository ->
                    assertFailsWith<CancellationException> {
                        when (operation) {
                            "window" -> repository.window()
                            "offers" -> repository.offers()
                            else -> repository.submit(emptyList())
                        }
                    }
                }
            }
        }

    @Test
    fun windowStateAndOfficialHourBoundsAreMappedWithoutInferringAvailabilityFromDates() =
        runTest {
            val statuses = listOf(
                "OPEN" to EnrollmentWindowState.Open,
                "UPCOMING" to EnrollmentWindowState.Upcoming,
                "CLOSED" to EnrollmentWindowState.Closed,
                "FUTURE_STATE" to EnrollmentWindowState.Unknown,
            )
            for ((wire, expected) in statuses) {
                withRepository({ request ->
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals("/api/enrollment/window", request.url.encodedPath)
                    jsonResponse(
                        """{"ok":true,"data":{"available":true,"window":{
                        "semester":"2026.2","state":"$wire",
                        "startDate":"2026-10-02T18:00:00-03:00","endDate":"2026-10-03T00:00:00-03:00",
                        "minHours":61,"maxHours":239,"useQueue":true,"courseId":901
                    }}}""",
                    )
                }) { repository ->
                    val availability = assertIs<Outcome.Ok<EnrollmentAvailability>>(repository.window()).value
                    assertEquals(true, availability.available)
                    assertEquals(expected, availability.window?.state)
                    assertEquals(61, availability.window?.minHours)
                    assertEquals(239, availability.window?.maxHours)
                    assertEquals(true, availability.window?.useQueue)
                    assertEquals("2026-10-02T18:00:00-03:00", availability.window?.startDate)
                    assertEquals("2026-10-03T00:00:00-03:00", availability.window?.endDate)
                }
            }
        }

    private suspend fun withRepository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
        block: suspend (EnrollmentRepositoryImpl) -> Unit,
    ) {
        val client = HttpClient(MockEngine(handler)) {
            expectSuccess = false
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        try {
            block(EnrollmentRepositoryImpl(EnrollmentApi(client), Logger))
        } finally {
            client.close()
        }
    }

    private fun MockRequestHandleScope.jsonResponse(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
}
