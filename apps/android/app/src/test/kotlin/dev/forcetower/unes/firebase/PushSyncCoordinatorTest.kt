package dev.forcetower.unes.firebase

import dev.forcetower.melon.core.common.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

// Waits use advanceTimeBy, not advanceUntilIdle: the coordinator's collector
// lives in backgroundScope, whose delayed tasks don't count toward the
// scheduler's idleness.
@OptIn(ExperimentalCoroutinesApi::class)
class PushSyncCoordinatorTest {

    @Test
    fun `a burst of requests collapses into one refresh`() = runTest {
        var runs = 0
        val coordinator = PushSyncCoordinator(
            scope = backgroundScope,
            hasSession = { true },
            refresh = { runs++; Outcome.Ok(Unit) },
        )

        repeat(5) { coordinator.request() }
        advanceTimeBy(10.seconds)

        assertEquals(1, runs)
    }

    @Test
    fun `each request restarts the debounce window`() = runTest {
        var runs = 0
        val coordinator = PushSyncCoordinator(
            scope = backgroundScope,
            hasSession = { true },
            refresh = { runs++; Outcome.Ok(Unit) },
        )

        coordinator.request()
        advanceTimeBy(1.5.seconds)
        assertEquals(0, runs)
        coordinator.request()
        advanceTimeBy(1.5.seconds)
        assertEquals(0, runs)
        advanceTimeBy(1.seconds)

        assertEquals(1, runs)
    }

    @Test
    fun `separate bursts refresh separately`() = runTest {
        var runs = 0
        val coordinator = PushSyncCoordinator(
            scope = backgroundScope,
            hasSession = { true },
            refresh = { runs++; Outcome.Ok(Unit) },
        )

        coordinator.request()
        advanceTimeBy(10.seconds)
        assertEquals(1, runs)
        coordinator.request()
        advanceTimeBy(10.seconds)

        assertEquals(2, runs)
    }

    @Test
    fun `a request arriving mid-refresh runs once more, never concurrently`() = runTest {
        var runs = 0
        var inFlight = 0
        var maxInFlight = 0
        val coordinator = PushSyncCoordinator(
            scope = backgroundScope,
            hasSession = { true },
            refresh = {
                runs++
                inFlight++
                maxInFlight = maxOf(maxInFlight, inFlight)
                delay(10.seconds)
                inFlight--
                Outcome.Ok(Unit)
            },
        )

        coordinator.request()
        // Past the debounce window — the first refresh is now in flight.
        advanceTimeBy(3.seconds)
        assertEquals(1, runs)
        coordinator.request()
        advanceTimeBy(30.seconds)

        assertEquals(2, runs)
        assertEquals(1, maxInFlight)
    }

    @Test
    fun `requests without a session refresh nothing`() = runTest {
        var runs = 0
        val coordinator = PushSyncCoordinator(
            scope = backgroundScope,
            hasSession = { false },
            refresh = { runs++; Outcome.Ok(Unit) },
        )

        coordinator.request()
        advanceTimeBy(10.seconds)

        assertEquals(0, runs)
    }

    @Test
    fun `a crashing refresh does not kill the pipeline`() = runTest {
        var runs = 0
        val coordinator = PushSyncCoordinator(
            scope = backgroundScope,
            hasSession = { true },
            refresh = {
                runs++
                if (runs == 1) error("boom")
                Outcome.Ok(Unit)
            },
        )

        coordinator.request()
        advanceTimeBy(10.seconds)
        assertEquals(1, runs)
        coordinator.request()
        advanceTimeBy(10.seconds)

        assertEquals(2, runs)
    }
}
