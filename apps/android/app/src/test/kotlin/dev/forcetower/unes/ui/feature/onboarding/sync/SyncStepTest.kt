package dev.forcetower.unes.ui.feature.onboarding.sync

import dev.forcetower.melon.core.common.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class SyncStepTest {
    @Test
    fun stalledPushRegistrationDoesNotBlockInitialSync() =
        runTest {
            assertEquals(StepResult.Ok, runAuthStepWork(backgroundScope, { Outcome.Ok(Unit) }, { awaitCancellation() }))
            assertEquals(0, testScheduler.currentTime)
        }

    @Test
    fun optionalActivityPingHasABoundedBestEffortWait() =
        runTest {
            assertEquals(StepResult.Ok, runAuthStepWork(backgroundScope, { awaitCancellation() }, {}))
            assertEquals(2_000, testScheduler.currentTime)
        }

    @Test
    fun stalledInitialSyncBecomesRetryableFailure() =
        runTest {
            val work = backgroundScope.async<StepResult> { awaitCancellation() }
            assertEquals(StepResult.Fail(false), awaitSyncStep(work, 1_000))
            assertEquals(1_000, testScheduler.currentTime)
        }

    @Test
    fun failedFetchNeverCountsAsCompletedStep() =
        runTest {
            val work = async<StepResult> { StepResult.Fail(false) }
            assertEquals(StepResult.Fail(false), awaitSyncStep(work, 1_000))
        }

    @Test
    fun successfulFetchCompletesWithoutWaitingForTimeout() =
        runTest {
            val work = async<StepResult> { StepResult.Ok }
            assertEquals(StepResult.Ok, awaitSyncStep(work, 1_000))
            assertEquals(0, testScheduler.currentTime)
        }
}
