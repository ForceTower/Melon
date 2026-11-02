package dev.forcetower.unes.ui.feature.commute

import dev.forcetower.melon.core.common.AppClock
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.usecase.FollowTransitArrivalUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.GetTransitNetworkUseCase
import dev.forcetower.unes.commute.CommuteLiveUpdates
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

// Following the bus in notifications from "Seguindo o 087".
@OptIn(ExperimentalCoroutinesApi::class)
internal class CommuteFollowViewModelTest {
    private val now = Instant.parse("2026-10-02T09:50:00Z")
    private val arrival = CommuteFixtures.board(now).arrivals[0]
    private val followed = MutableStateFlow<String?>(null)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun theNotificationStartsFromWhatTheScreenShowsAndStopsAgain() =
        runTest {
            val liveUpdates = liveUpdates(canPost = true)
            coEvery { liveUpdates.start(any(), any(), any(), any(), any()) } answers {
                followed.value = firstArg<TransitArrival>().id
                true
            }
            coEvery { liveUpdates.stop(any()) } answers { followed.value = null }
            val model = openedModel(liveUpdates)
            assertTrue(model.state.value.showsNotificationToggle)

            model.onIntent(CommuteFollowIntent.NotificationToggled)
            advanceUntilIdle()
            val route = CommuteFixtures.network.route("087", TransitDirection.ToCampus)!!
            val lineName = CommuteFixtures.network.line("087")!!.name
            coVerify { liveUpdates.start(arrival, route, lineName, CommuteFixtures.MY_STOP_ID, null) }
            assertTrue(model.state.value.followsInNotifications)

            model.onIntent(CommuteFollowIntent.NotificationToggled)
            advanceUntilIdle()
            coVerify { liveUpdates.stop(null) }
            assertFalse(model.state.value.followsInNotifications)
        }

    @Test
    fun notificationsTurnedOffPointToSettings() =
        runTest {
            val liveUpdates = liveUpdates(canPost = false)
            coEvery { liveUpdates.start(any(), any(), any(), any(), any()) } returns false
            val model = openedModel(liveUpdates)

            model.onIntent(CommuteFollowIntent.NotificationToggled)
            advanceUntilIdle()

            assertTrue(model.state.value.notificationsDenied)
            assertFalse(model.state.value.followsInNotifications)
        }

    private fun liveUpdates(canPost: Boolean): CommuteLiveUpdates {
        val liveUpdates = mockk<CommuteLiveUpdates>()
        every { liveUpdates.followedArrivalId } returns followed
        every { liveUpdates.canPost() } returns canPost
        return liveUpdates
    }

    private fun TestScope.openedModel(liveUpdates: CommuteLiveUpdates): CommuteFollowViewModel {
        val network = mockk<GetTransitNetworkUseCase>()
        coEvery { network() } returns Outcome.Ok(CommuteFixtures.network)
        val model = CommuteFollowViewModel(
            followTransitArrival = mockk<FollowTransitArrivalUseCase>(),
            getTransitNetwork = network,
            locationClient = mockk<DeviceLocationClient>(relaxed = true),
            liveUpdates = liveUpdates,
            clock = AppClock(),
        )
        model.onIntent(CommuteFollowIntent.Seed(arrival, TransitDirection.ToCampus, CommuteFixtures.MY_STOP_ID, now))
        model.onIntent(
            CommuteFollowIntent.Open(
                lineNumber = arrival.lineNumber,
                direction = TransitDirection.ToCampus,
                stopId = CommuteFixtures.MY_STOP_ID,
                arrivalId = arrival.id,
                vehicleId = arrival.vehicleId,
            ),
        )
        advanceUntilIdle()
        return model
    }
}
