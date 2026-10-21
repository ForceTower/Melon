package dev.forcetower.melon.feature.transit

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.storage.KeyValueStorage
import dev.forcetower.melon.feature.transit.data.CommutePlanRepository
import dev.forcetower.melon.feature.transit.data.network.TransitService
import dev.forcetower.melon.feature.transit.domain.model.CommutePlan
import dev.forcetower.melon.feature.transit.domain.model.TransitArrivalState
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitFeedStatus
import dev.forcetower.melon.feature.transit.domain.model.TransitFollow
import dev.forcetower.melon.feature.transit.domain.model.TransitNetwork
import dev.forcetower.melon.feature.transit.domain.model.TransitStopBoard
import dev.forcetower.melon.feature.transit.domain.model.TransitTodaySlotStatus
import dev.forcetower.melon.feature.transit.domain.model.TransitVehiclePhase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Decodes the shared synthetic `contracts/v1/transit.json` through the real
// service, the way the app does, so a wire change on the server side fails
// here before it blanks a board on a phone.
internal class TransitContractTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val fixture: Map<String, JsonElement> by lazy {
        assertNotNull(javaClass.getResourceAsStream("/v1/transit.json")).bufferedReader().use {
            json.parseToJsonElement(it.readText()).jsonObject
        }
    }

    @Test
    fun sharedSyntheticContractDecodesNetworkArrivalsAndFollow() =
        runTest {
            val service = service { request ->
                when {
                    request.url.encodedPath.endsWith("/network") -> fixture.getValue("network")
                    request.url.encodedPath.contains("/arrivals") -> fixture.getValue("arrivals")
                    else -> fixture.getValue("follow")
                }
            }

            val network = assertIs<Outcome.Ok<TransitNetwork>>(service.network()).value
            assertEquals(listOf("087"), network.lines.map { it.number })
            val inbound = assertNotNull(network.route("087", TransitDirection.ToCampus))
            assertEquals(listOf("4975", "9001", "5670"), inbound.stops.map { it.id })
            assertEquals(35..40, inbound.typicalMinutes)
            assertEquals(listOf("4975", "9001"), network.boardingStops(TransitDirection.ToCampus).map { it.id })
            assertEquals("2452", network.campusDepartureStop?.id)
            assertEquals(listOf("087"), network.linesServing("9001", TransitDirection.ToCampus))

            val board = assertIs<Outcome.Ok<TransitStopBoard>>(
                service.arrivals("9001", TransitDirection.ToCampus, listOf("087")),
            ).value
            assertEquals(TransitFeedStatus.Ok, board.feed.status)
            assertEquals(
                listOf(TransitArrivalState.Live, TransitArrivalState.Waiting, TransitArrivalState.Historical),
                board.arrivals.map { it.state },
            )
            val live = board.arrivals[0]
            assertEquals(TransitVehiclePhase.EnRoute, live.phase)
            assertEquals(4, live.stopsAway)
            assertEquals(Instant.parse("2026-10-02T09:58:00.000Z"), live.earliest)
            assertEquals(listOf(1, 2, 4, 9, 7, 3, 1), live.basis?.counts)
            assertTrue(board.arrivals[2].isFlaky)
            assertEquals(
                listOf(TransitTodaySlotStatus.Passed, TransitTodaySlotStatus.Next, TransitTodaySlotStatus.Upcoming),
                board.today.map { it.status },
            )
            assertTrue(board.serviceToday)
            assertNull(board.nextService)

            val follow = assertIs<Outcome.Ok<TransitFollow>>(
                service.follow("087", TransitDirection.ToCampus, "9001", live.id, live.vehicleId),
            ).value
            assertEquals(listOf("90001", "90002"), follow.vehicles.map { it.id })
            assertEquals(20.0, follow.vehicles[0].heading)
            assertNull(follow.arrival)
        }

    @Test
    fun planRoundTripsThroughTheMirrorAndSendsBothDirections() =
        runTest {
            val sentBodies = mutableListOf<String>()
            val service = service { request ->
                if (request.method == HttpMethod.Put) sentBodies += (request.body as TextContent).text
                // A removal answers with the now-empty plan, not with no data.
                if (request.method == HttpMethod.Delete) {
                    json.parseToJsonElement("{}")
                } else {
                    fixture.getValue("plan")
                }
            }
            val storage = FakeStorage()
            val repository = CommutePlanRepository(service, storage, json, Logger)

            assertTrue(repository.observe().first().isEmpty)
            assertIs<Outcome.Ok<Unit>>(repository.refresh())
            val mirrored = repository.observe().first()
            assertEquals("Rua de Exemplo, 100", mirrored.toCampus?.stopName)
            assertEquals(listOf("087"), mirrored.fromCampus?.lineNumbers)

            // A fresh repository hydrates the same plan from the stored snapshot.
            assertEquals(mirrored, CommutePlanRepository(service, storage, json, Logger).observe().first())

            val saved = assertIs<Outcome.Ok<CommutePlan>>(
                repository.save(CommutePlan(toCampus = mirrored.toCampus)),
            ).value
            assertEquals(mirrored, saved)
            val body = json.parseToJsonElement(sentBodies.single()).jsonObject
            assertEquals("9001", body.getValue("toCampus").jsonObject.getValue("stopId").jsonPrimitive.content)
            assertEquals(JsonNull, body.getValue("fromCampus"))

            assertIs<Outcome.Ok<Unit>>(repository.remove())
            assertTrue(repository.observe().first().isEmpty)
            assertNull(storage.get("transit.commute_plan"))
        }

    // A refresh in flight at logout answers with the previous account's
    // plan; the mirror must not take it.
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun aRefreshOutlivingTheLogoutCannotRestoreThePlan() =
        runTest {
            val answer = CompletableDeferred<Unit>()
            val service = service {
                answer.await()
                fixture.getValue("plan")
            }
            val storage = FakeStorage()
            val repository = CommutePlanRepository(service, storage, json, Logger)

            val refresh = async { repository.refresh() }
            runCurrent()
            repository.clear()
            answer.complete(Unit)
            assertIs<Outcome.Ok<Unit>>(refresh.await())

            assertTrue(repository.observe().first().isEmpty)
            assertNull(storage.get("transit.commute_plan"))
        }

    private fun service(data: suspend (HttpRequestData) -> JsonElement): TransitService {
        val client = HttpClient(
            MockEngine { request ->
                respond(
                    """{"ok":true,"data":${data(request)}}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) { json(json) }
        }
        return TransitService(client)
    }

    private class FakeStorage : KeyValueStorage {
        private val values = mutableMapOf<String, String>()

        override suspend fun get(key: String): String? = values[key]

        override suspend fun put(
            key: String,
            value: String,
        ) {
            values[key] = value
        }

        override suspend fun remove(key: String) {
            values.remove(key)
        }
    }
}
