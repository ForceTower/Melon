package dev.forcetower.melon.feature.sync

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.MessagePageResult
import dev.forcetower.melon.core.sync.domain.model.OnboardingStatus
import dev.forcetower.melon.core.sync.domain.model.OnboardingStatus.PhaseStatus.State
import dev.forcetower.melon.core.sync.domain.model.SemesterSummary
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.core.sync.domain.repository.MirrorRepository
import dev.forcetower.melon.core.sync.domain.repository.SyncStateRepository
import dev.forcetower.melon.feature.sync.domain.usecase.BackfillMirrorUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

// The completion flag gates every later backfill until logout, so it may only
// flip once the server's Phase 2 has settled — never when polling gives up.
class BackfillMirrorUseCaseTest {

    @Test
    fun backfill_that_gives_up_polling_stays_incomplete_and_polls_again_next_time() =
        runTest {
            val mirror = PollingMirrorRepository(status(semesters = State.Running))
            val syncState = InMemorySyncStateRepository()
            val backfill = BackfillMirrorUseCase(mirror, syncState, Logger)

            assertIs<Outcome.Ok<Unit>>(backfill())
            assertFalse(syncState.getBackfillMirrorComplete())
            assertEquals(listOf("sem1"), mirror.syncedSemesters)

            val pollsAfterFirstRun = mirror.statusPolls
            backfill()
            assertTrue(mirror.statusPolls > pollsAfterFirstRun)
        }

    @Test
    fun failed_phase_one_stops_polling_without_marking_the_backfill_complete() =
        runTest {
            val mirror = PollingMirrorRepository(
                status(initial = State.Failed, semesters = State.Pending, messages = State.Pending),
            )
            val syncState = InMemorySyncStateRepository()

            BackfillMirrorUseCase(mirror, syncState, Logger)()

            assertEquals(1, mirror.statusPolls)
            assertFalse(syncState.getBackfillMirrorComplete())
        }

    @Test
    fun unrecognised_phase_state_does_not_mark_the_backfill_complete() =
        runTest {
            val mirror = PollingMirrorRepository(status(messages = State.Unknown))
            val syncState = InMemorySyncStateRepository()

            BackfillMirrorUseCase(mirror, syncState, Logger)()

            assertFalse(syncState.getBackfillMirrorComplete())
        }

    @Test
    fun settled_phase_two_marks_the_backfill_complete_and_later_runs_skip() =
        runTest {
            val mirror = PollingMirrorRepository(status())
            val syncState = InMemorySyncStateRepository()
            val backfill = BackfillMirrorUseCase(mirror, syncState, Logger)

            backfill()
            assertTrue(syncState.getBackfillMirrorComplete())
            assertEquals(listOf("sem1"), mirror.syncedSemesters)

            backfill()
            assertEquals(1, mirror.statusPolls)
        }

    private fun status(
        initial: State = State.Done,
        semesters: State = State.Done,
        messages: State = State.Done,
    ) = OnboardingStatus(
        courseLinked = true,
        initial = OnboardingStatus.InitialStatus(state = initial, appliedSemesters = 1),
        semesters = OnboardingStatus.PhaseStatus(state = semesters),
        messages = OnboardingStatus.PhaseStatus(state = messages),
        activeSemesterReady = initial == State.Done || initial == State.Failed,
    )
}

private class PollingMirrorRepository(
    private val status: OnboardingStatus,
) : MirrorRepository {
    var statusPolls = 0
    val syncedSemesters = mutableListOf<String>()

    override suspend fun fetchOnboardingStatus(): Outcome<OnboardingStatus, SyncError> {
        statusPolls += 1
        return Outcome.Ok(status)
    }

    override suspend fun syncSemesterList(): Outcome<List<SemesterSummary>, SyncError> =
        Outcome.Ok(
            listOf(
                SemesterSummary(
                    id = "sem1",
                    code = "2020.1",
                    desc = "Semestre 2020.1",
                    startDate = "2020-02-01",
                    endDate = "2020-06-30",
                    track = null,
                    dirtyAt = null,
                ),
            ),
        )

    override suspend fun syncSemester(semesterId: String): Outcome<Unit, SyncError> {
        syncedSemesters += semesterId
        return Outcome.Ok(Unit)
    }

    override suspend fun syncMessages(
        since: String?,
        cursor: String?,
    ): Outcome<MessagePageResult, SyncError> = Outcome.Ok(MessagePageResult(appliedCount = 0, nextCursor = null))

    override suspend fun syncProfile(): Outcome<Unit, SyncError> = Outcome.Ok(Unit)

    override suspend fun listStaleMirroredSemesterIds(): List<String> = emptyList()

    override suspend fun syncCalendarEvents(): Outcome<Int, SyncError> = Outcome.Ok(0)

    override suspend fun pingActivity(): Outcome<Unit, SyncError> = Outcome.Ok(Unit)

    override suspend fun syncMyCredentials(): Outcome<Unit, SyncError> = Outcome.Ok(Unit)
}

private class InMemorySyncStateRepository : SyncStateRepository {
    private var onboardingComplete = false
    private var backfillMirrorComplete = false

    override suspend fun getOnboardingComplete(): Boolean = onboardingComplete

    override suspend fun setOnboardingComplete(value: Boolean) {
        onboardingComplete = value
    }

    override suspend fun getBackfillMirrorComplete(): Boolean = backfillMirrorComplete

    override suspend fun setBackfillMirrorComplete(value: Boolean) {
        backfillMirrorComplete = value
    }

    override suspend fun reset() {
        onboardingComplete = false
        backfillMirrorComplete = false
    }
}
