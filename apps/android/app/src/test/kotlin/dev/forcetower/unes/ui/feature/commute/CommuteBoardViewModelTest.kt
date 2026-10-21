package dev.forcetower.unes.ui.feature.commute

import dev.forcetower.melon.core.analytics.NoOpAnalytics
import dev.forcetower.melon.core.common.AppClock
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.transit.domain.model.CommutePlan
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitError
import dev.forcetower.melon.feature.transit.domain.usecase.GetStopArrivalsUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.GetTransitNetworkUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.ObserveCommutePlanUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.RefreshCommutePlanUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

// Which leg the board shows across route entries and plan changes.
@OptIn(ExperimentalCoroutinesApi::class)
internal class CommuteBoardViewModelTest {
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
    fun aLegEditedAwayHandsTheBoardToTheRemainingOne() =
        runTest {
            val plans = MutableStateFlow(CommuteFixtures.plan)
            val model = model(plans)
            model.onIntent(CommuteBoardIntent.Open(TransitDirection.ToCampus))
            advanceUntilIdle()
            model.onIntent(CommuteBoardIntent.DirectionPicked(TransitDirection.FromCampus))
            assertEquals(TransitDirection.FromCampus, model.state.value.direction)

            plans.value = CommutePlan(toCampus = CommuteFixtures.plan.toCampus)
            advanceUntilIdle()

            assertEquals(TransitDirection.ToCampus, model.state.value.direction)
            assertNotNull(model.state.value.leg)
        }

    @Test
    fun aPopBackKeepsThePickedDirectionAndAClosedRouteForgetsIt() =
        runTest {
            val model = model(MutableStateFlow(CommuteFixtures.plan))
            model.onIntent(CommuteBoardIntent.Open(TransitDirection.ToCampus))
            advanceUntilIdle()
            model.onIntent(CommuteBoardIntent.DirectionPicked(TransitDirection.FromCampus))

            model.onIntent(CommuteBoardIntent.Open(TransitDirection.ToCampus))
            advanceUntilIdle()
            assertEquals(TransitDirection.FromCampus, model.state.value.direction)

            model.onIntent(CommuteBoardIntent.Closed)
            model.onIntent(CommuteBoardIntent.Open(TransitDirection.ToCampus))
            advanceUntilIdle()
            assertEquals(TransitDirection.ToCampus, model.state.value.direction)
        }

    private fun model(plans: MutableStateFlow<CommutePlan>): CommuteBoardViewModel {
        val observePlan = mockk<ObserveCommutePlanUseCase>()
        every { observePlan() } returns plans
        val refreshPlan = mockk<RefreshCommutePlanUseCase>()
        coEvery { refreshPlan() } returns Outcome.Ok(Unit)
        val network = mockk<GetTransitNetworkUseCase>()
        coEvery { network() } returns Outcome.Ok(CommuteFixtures.network)
        val arrivals = mockk<GetStopArrivalsUseCase>()
        coEvery { arrivals(any(), any(), any()) } returns Outcome.Err(TransitError.Connection)
        return CommuteBoardViewModel(
            observeCommutePlan = observePlan,
            refreshCommutePlan = refreshPlan,
            getTransitNetwork = network,
            getStopArrivals = arrivals,
            clock = AppClock(),
            analytics = NoOpAnalytics,
        )
    }
}
