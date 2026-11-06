package dev.forcetower.unes.commute

import dev.forcetower.melon.core.common.AppClock
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitError
import dev.forcetower.melon.feature.transit.domain.model.TransitLiveUpdate
import dev.forcetower.melon.feature.transit.domain.model.TransitLiveUpdateRegistration
import dev.forcetower.melon.feature.transit.domain.usecase.DecodeTransitLiveUpdateUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.RegisterTransitLiveUpdateUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.UnregisterTransitLiveUpdateUseCase
import dev.forcetower.unes.firebase.PushIdentifierStore
import dev.forcetower.unes.ui.feature.commute.CommuteFixtures
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

// The followed bus's lifecycle outside the app: registered with the backend,
// redrawn by pushes and alarms, and ended even when the backend's end never
// comes.
@OptIn(ExperimentalCoroutinesApi::class)
internal class CommuteLiveUpdatesTest {
    private val now = Instant.parse("2026-10-02T09:50:00Z")
    private val clock = MutableClock(now)
    private val saved = MutableStateFlow<CommuteLiveRecord?>(null)
    private var token: String? = "fcm-token"
    private val alarms = mutableListOf<Instant>()

    private val store = mockk<CommuteLiveUpdateStore>()
    private val notifier = mockk<CommuteLiveUpdateNotifier>(relaxed = true)
    private val pushIdentifiers = mockk<PushIdentifierStore>()
    private val registerLiveUpdate = mockk<RegisterTransitLiveUpdateUseCase>()
    private val unregisterLiveUpdate = mockk<UnregisterTransitLiveUpdateUseCase>()

    @BeforeTest
    fun setup() {
        every { store.record } returns saved
        coEvery { store.current() } answers { saved.value }
        coEvery { store.save(any()) } answers { saved.value = firstArg() }
        coEvery { store.clear() } answers { saved.value = null }
        every { notifier.canPost() } returns true
        every { notifier.scheduleStale(any(), capture(alarms)) } just Runs
        coEvery { pushIdentifiers.token() } answers { token }
        coEvery { registerLiveUpdate(any()) } returns Outcome.Ok(Unit)
        coEvery { unregisterLiveUpdate(any()) } returns Outcome.Ok(Unit)
    }

    @Test
    fun aFollowNoPushEverReachesEndsAtItsExpiry() =
        runTest {
            token = null
            val live = liveUpdates()
            assertTrue(live.start())
            val activityId = assertNotNull(saved.value).activityId

            clock.instant = alarms.last()
            live.onStale(activityId)
            assertNotNull(saved.value)
            verify { notifier.show(any(), any(), stale = true, any()) }

            clock.instant = alarms.last()
            live.onStale(activityId)
            assertNull(saved.value)
            verify { notifier.cancel() }
        }

    @Test
    fun anExpiredFollowEndsWhenTheAppOpens() =
        runTest {
            val live = liveUpdates()
            live.start()
            val activityId = assertNotNull(saved.value).activityId

            clock.instant = now + 1.days
            assertNull(live.followedArrivalId.first())
            live.resume()

            assertNull(saved.value)
            verify { notifier.cancel() }
            coVerify(exactly = 1) { unregisterLiveUpdate(activityId) }
        }

    @Test
    fun stoppingWhileTheRegistrationIsInFlightStillUnregistersIt() =
        runTest {
            val response = CompletableDeferred<Outcome<Unit, TransitError>>()
            coEvery { registerLiveUpdate(any()) } coAnswers { response.await() }
            val live = liveUpdates()
            val starting = launch { live.start() }
            runCurrent()
            val activityId = assertNotNull(saved.value).activityId

            live.stop()
            response.complete(Outcome.Ok(Unit))
            starting.join()

            coVerify(exactly = 1) { unregisterLiveUpdate(activityId) }
        }

    @Test
    fun replacingAFollowWhileItRegistersUnregistersTheOldOne() =
        runTest {
            val response = CompletableDeferred<Outcome<Unit, TransitError>>()
            var calls = 0
            coEvery { registerLiveUpdate(any()) } coAnswers {
                if (calls++ == 0) response.await() else Outcome.Ok(Unit)
            }
            val live = liveUpdates()
            val first = launch { live.start() }
            runCurrent()
            val firstId = assertNotNull(saved.value).activityId

            live.start()
            val secondId = assertNotNull(saved.value).activityId
            response.complete(Outcome.Ok(Unit))
            first.join()

            coVerify(exactly = 1) { unregisterLiveUpdate(firstId) }
            coVerify(exactly = 0) { unregisterLiveUpdate(secondId) }
            assertEquals(secondId, saved.value?.activityId)
        }

    @Test
    fun aRotatedTokenRegistersTheFollowAgain() =
        runTest {
            val live = liveUpdates()
            live.start()
            live.resume()
            coVerify(exactly = 1) { registerLiveUpdate(any()) }

            token = "rotated-fcm-token"
            live.resume()

            coVerify(exactly = 1) { registerLiveUpdate(match { it.deviceToken == "rotated-fcm-token" }) }
            assertEquals("rotated-fcm-token", saved.value?.registeredToken)
        }

    @Test
    fun aPushPastTheExpiryEndsTheFollowInsteadOfRedrawingIt() =
        runTest {
            val live = liveUpdates()
            live.start()
            val record = assertNotNull(saved.value)

            clock.instant = record.expiry
            live.onPush(
                mapOf(
                    "activityId" to record.activityId,
                    "event" to "update",
                    "content" to "{}",
                    "timestamp" to clock.instant.epochSeconds.toString(),
                ),
            )

            assertNull(saved.value)
            verify(exactly = 1) { notifier.show(any(), any(), any(), any()) }
            coVerify(exactly = 1) { unregisterLiveUpdate(record.activityId) }
        }

    @Test
    fun aRegistrationThatNeverWentThroughIsRetriedOnceThereIsAToken() =
        runTest {
            token = null
            val live = liveUpdates()
            live.start()
            coVerify(exactly = 0) { registerLiveUpdate(any()) }

            token = "fcm-token"
            live.resume()
            live.resume()

            coVerify(exactly = 1) { registerLiveUpdate(any<TransitLiveUpdateRegistration>()) }
        }

    private suspend fun CommuteLiveUpdates.start(): Boolean {
        val follow = CommuteFixtures.follow(clock.instant)
        return start(
            arrival = follow.arrival!!,
            route = CommuteFixtures.network.route("087", TransitDirection.ToCampus)!!,
            lineName = "Expresso Universitário",
            stopId = CommuteFixtures.MY_STOP_ID,
            progressMeters = null,
        )
    }

    private fun liveUpdates(): CommuteLiveUpdates {
        val decodeLiveUpdate = mockk<DecodeTransitLiveUpdateUseCase>()
        every { decodeLiveUpdate(any()) } answers {
            TransitLiveUpdate.of(CommuteFixtures.follow(clock.instant).arrival!!, progressMeters = null)
        }
        return CommuteLiveUpdates(
            store = store,
            notifier = notifier,
            pushIdentifiers = pushIdentifiers,
            registerLiveUpdate = registerLiveUpdate,
            unregisterLiveUpdate = unregisterLiveUpdate,
            decodeLiveUpdate = decodeLiveUpdate,
            clock = AppClock(clock),
        )
    }
}

private class MutableClock(
    var instant: Instant,
) : Clock {
    override fun now() = instant
}
