package dev.forcetower.melon.feature.enrollment

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.enrollment.data.dto.SubmitEnrollmentRequest
import dev.forcetower.melon.feature.enrollment.data.network.EnrollmentApi
import dev.forcetower.melon.feature.enrollment.data.repository.EnrollmentRepositoryImpl
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentAvailability
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentOffers
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentShift
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSlot
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindowState
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

internal class EnrollmentContractTest {
    @Test
    fun sharedSyntheticContractPreservesWindowSavedSectionsSchedulesAndWaitlistRules() =
        runTest {
            val json = Json { ignoreUnknownKeys = true }
            val fixture = assertNotNull(javaClass.getResourceAsStream("/v1/enrollment.json")).bufferedReader().use {
                json.parseToJsonElement(it.readText()).jsonObject
            }
            val client = HttpClient(
                MockEngine { request ->
                    val data = fixture.getValue(request.url.encodedPath.substringAfterLast('/'))
                    respond(
                        """{"ok":true,"data":$data}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ) {
                install(ContentNegotiation) { json(json) }
            }
            try {
                val repository = EnrollmentRepositoryImpl(EnrollmentApi(client), Logger)
                val availability = assertIs<Outcome.Ok<EnrollmentAvailability>>(repository.window()).value
                assertTrue(availability.available)
                val window = assertNotNull(availability.window)
                assertEquals(EnrollmentWindowState.Open, window.state)
                assertEquals(60, window.minHours)
                assertEquals(120, window.maxHours)
                assertTrue(window.useQueue)

                val offers = assertIs<Outcome.Ok<EnrollmentOffers>>(repository.offers()).value.disciplines
                assertEquals(listOf(201L, 202L, 203L), offers.map { it.id })
                assertEquals(listOf(60, 60, 60), offers.map { it.workload })
                val saved = offers[0].sections.single()
                assertTrue(saved.selected)
                assertTrue(saved.allowsOtherDefault)
                assertEquals(EnrollmentShift.Morning, saved.meetings.single().shift)
                assertEquals(listOf(EnrollmentSlot(1, "08:00", "10:00")), saved.meetings.single().slots)
                val conflicting = offers[1].sections.single()
                assertEquals(listOf(EnrollmentSlot(1, "09:00", "11:00")), conflicting.meetings.single().slots)
                val full = offers[2].sections.single()
                assertEquals(0, full.vacancies)
                assertEquals(20, full.proposalsCount)
                assertEquals(4, full.waitlistCount)
                assertFalse(full.selected)
                assertTrue(offers[2].prerequisites.single().met)

                val replacement = json.decodeFromJsonElement<SubmitEnrollmentRequest>(fixture.getValue("submission"))
                assertEquals(listOf(2031L), replacement.selections.map { it.sectionId })
                assertTrue(replacement.selections.single().waitlist)
                assertFalse(replacement.selections.single().allowsOther)
            } finally {
                client.close()
            }
        }
}
