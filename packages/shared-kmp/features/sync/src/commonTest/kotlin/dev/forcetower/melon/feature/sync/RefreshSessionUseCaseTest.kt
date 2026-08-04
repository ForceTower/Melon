package dev.forcetower.melon.feature.sync

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.MessagePageResult
import dev.forcetower.melon.core.sync.domain.model.OnboardingStatus
import dev.forcetower.melon.core.sync.domain.model.SemesterSummary
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.core.sync.domain.repository.MirrorRepository
import dev.forcetower.melon.feature.sync.domain.usecase.RefreshSessionUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// The refresh must pull the active semester AND re-pull any mirrored
// semester the worker touched since its payload was applied — a result
// posted after a semester ends would otherwise never reach the mirror.
class RefreshSessionUseCaseTest {

    // Both windows are in the past so "active" always resolves through the
    // newest-by-startDate fallback, independent of the date the test runs.
    private val past = summary("sem1", start = "2020-02-01", end = "2020-06-30", dirtyAt = "2020-07-22T21:14:00.000Z")
    private val newest = summary("sem2", start = "2020-08-01", end = "2020-12-15", dirtyAt = "2020-12-20T10:00:00.000Z")

    @Test
    fun refresh_repulls_stale_mirrored_semesters_alongside_the_active_one() = runTest {
        val mirror = FakeMirrorRepository(summaries = listOf(newest, past), staleIds = listOf("sem1"))
        val result = RefreshSessionUseCase(mirror, Logger)()

        assertIs<Outcome.Ok<Unit>>(result)
        assertEquals(listOf("sem2", "sem1"), mirror.syncedSemesters)
    }

    @Test
    fun refresh_without_stale_semesters_only_pulls_the_active_one() = runTest {
        val mirror = FakeMirrorRepository(summaries = listOf(newest, past), staleIds = emptyList())
        val result = RefreshSessionUseCase(mirror, Logger)()

        assertIs<Outcome.Ok<Unit>>(result)
        assertEquals(listOf("sem2"), mirror.syncedSemesters)
    }

    @Test
    fun refresh_dedups_an_active_semester_that_is_also_stale() = runTest {
        val mirror = FakeMirrorRepository(summaries = listOf(newest, past), staleIds = listOf("sem2"))
        val result = RefreshSessionUseCase(mirror, Logger)()

        assertIs<Outcome.Ok<Unit>>(result)
        assertEquals(listOf("sem2"), mirror.syncedSemesters)
    }

    private fun summary(id: String, start: String, end: String, dirtyAt: String?) = SemesterSummary(
        id = id,
        code = id,
        desc = "Semestre $id",
        startDate = start,
        endDate = end,
        track = null,
        dirtyAt = dirtyAt,
    )
}

private class FakeMirrorRepository(
    private val summaries: List<SemesterSummary>,
    private val staleIds: List<String>,
) : MirrorRepository {
    val syncedSemesters = mutableListOf<String>()

    override suspend fun syncProfile(): Outcome<Unit, SyncError> = Outcome.Ok(Unit)

    override suspend fun syncSemesterList(): Outcome<List<SemesterSummary>, SyncError> = Outcome.Ok(summaries)

    override suspend fun syncSemester(semesterId: String): Outcome<Unit, SyncError> {
        syncedSemesters += semesterId
        return Outcome.Ok(Unit)
    }

    override suspend fun listStaleMirroredSemesterIds(): List<String> = staleIds

    override suspend fun fetchOnboardingStatus(): Outcome<OnboardingStatus, SyncError> =
        Outcome.Err(SyncError.Unexpected)

    override suspend fun syncMessages(since: String?, cursor: String?): Outcome<MessagePageResult, SyncError> =
        Outcome.Ok(MessagePageResult(appliedCount = 0, nextCursor = null))

    override suspend fun syncCalendarEvents(): Outcome<Int, SyncError> = Outcome.Ok(0)

    override suspend fun pingActivity(): Outcome<Unit, SyncError> = Outcome.Ok(Unit)

    override suspend fun syncMyCredentials(): Outcome<Unit, SyncError> = Outcome.Ok(Unit)
}
