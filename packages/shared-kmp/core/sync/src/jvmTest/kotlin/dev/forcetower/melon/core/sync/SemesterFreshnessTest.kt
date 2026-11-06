package dev.forcetower.melon.core.sync

import dev.forcetower.melon.core.common.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray

// A semester payload is only as fresh as the `dirtyAt` in effect when it was
// requested. A semester list that lands while the payload is in flight (a
// concurrent refresh) must not make the older payload look current.
internal class SemesterFreshnessTest {
    private val past = contractFixture("/v1/disciplines.json").getValue("past").jsonObject
    private val semester = past.getValue("semester").jsonObject
    private val semesterId = semester.getValue("id").jsonPrimitive.content

    @Test
    fun payloadRequestedBeforeANewerListLandedStaysStale() {
        var listedDirtyAt = BEFORE_WORKER_RUN
        mirrorTest(
            respond = { request, repository ->
                when (request.url.encodedPath) {
                    "/api/sync/semesters" -> respondEnvelope(semesterList(listedDirtyAt))
                    "/api/sync/semesters/$semesterId" -> {
                        listedDirtyAt = AFTER_WORKER_RUN
                        repository.syncSemesterList()
                        respondEnvelope(past.toString())
                    }
                    else -> error("Unexpected request ${request.url}")
                }
            },
        ) { database, repository ->
            repository.syncSemesterList()

            assertIs<Outcome.Ok<Unit>>(repository.syncSemester(semesterId))

            assertEquals(AFTER_WORKER_RUN, database.semesterDao().get(semesterId)?.dirtyAt)
            assertEquals(listOf(semesterId), database.semesterDao().listStaleMirroredIds())
        }
    }

    @Test
    fun payloadRequestedUnderTheCurrentListIsFresh() =
        mirrorTest(
            respond = { request, _ ->
                when (request.url.encodedPath) {
                    "/api/sync/semesters" -> respondEnvelope(semesterList(AFTER_WORKER_RUN))
                    "/api/sync/semesters/$semesterId" -> respondEnvelope(past.toString())
                    else -> error("Unexpected request ${request.url}")
                }
            },
        ) { database, repository ->
            repository.syncSemesterList()

            assertIs<Outcome.Ok<Unit>>(repository.syncSemester(semesterId))

            assertEquals(AFTER_WORKER_RUN, database.semesterDao().get(semesterId)?.appliedDirtyAt)
            assertEquals(emptyList(), database.semesterDao().listStaleMirroredIds())
        }

    private fun semesterList(dirtyAt: String): String =
        buildJsonObject {
            putJsonArray("semesters") {
                add(JsonObject(semester + ("dirtyAt" to JsonPrimitive(dirtyAt))))
            }
        }.toString()

    private companion object {
        const val BEFORE_WORKER_RUN = "2026-07-01T10:00:00.000Z"
        const val AFTER_WORKER_RUN = "2026-07-22T21:14:00.000Z"
    }
}
