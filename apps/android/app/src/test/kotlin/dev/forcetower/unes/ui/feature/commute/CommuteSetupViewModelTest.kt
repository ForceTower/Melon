package dev.forcetower.unes.ui.feature.commute

import dev.forcetower.melon.core.analytics.NoOpAnalytics
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.transit.domain.model.CommutePlan
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitError
import dev.forcetower.melon.feature.transit.domain.usecase.GetTransitNetworkUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.ObserveCommutePlanUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.RemoveCommutePlanUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.SaveCommutePlanUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

// The setup session across its three steps: the directions chosen first,
// the stop, the lines prefilled from the catalog, and the whole plan sent
// in one PUT at the end — or handed to the next direction's lines step.
@OptIn(ExperimentalCoroutinesApi::class)
internal class CommuteSetupViewModelTest {
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
    fun bothDirectionsWalkStopThenLinesTwiceAndSaveThePlanWhole() =
        runTest {
            val saved = slot<CommutePlan>()
            val save = mockk<SaveCommutePlanUseCase>()
            coEvery { save(capture(saved)) } answers { Outcome.Ok(saved.captured) }
            val model = model(save = save)
            val effects = mutableListOf<CommuteSetupEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.effects.toList(effects) }

            model.onIntent(CommuteSetupIntent.Begin(CommutePlan()))
            advanceUntilIdle()
            assertEquals(listOf(TransitDirection.ToCampus), model.state.value.directions)
            assertEquals(3, model.state.value.totalSteps)

            model.onIntent(CommuteSetupIntent.DirectionToggled(TransitDirection.FromCampus))
            assertEquals(4, model.state.value.totalSteps)
            model.onIntent(CommuteSetupIntent.ContinueFromDirection)
            advanceUntilIdle()
            assertEquals(CommuteSetupEffect.GoToStop, effects.last())

            model.onIntent(CommuteSetupIntent.StopSelected(CommuteFixtures.MY_STOP_ID))
            model.onIntent(CommuteSetupIntent.ContinueFromStop)
            advanceUntilIdle()
            assertEquals(CommuteSetupEffect.GoToLines(TransitDirection.ToCampus), effects.last())

            // Every line serving the stop is on by default; the 003 doesn't stop there.
            model.onIntent(CommuteSetupIntent.OpenLines(TransitDirection.ToCampus))
            assertEquals(listOf("087", "088"), model.state.value.selectedLines)
            assertEquals(5, model.state.value.walkMinutes)
            model.onIntent(CommuteSetupIntent.LineToggled("003"))
            model.onIntent(CommuteSetupIntent.LineToggled("088"))
            model.onIntent(CommuteSetupIntent.WalkChanged(8))
            assertEquals(listOf("087"), model.state.value.selectedLines)
            model.onIntent(CommuteSetupIntent.SaveTapped)
            advanceUntilIdle()
            assertEquals(CommuteSetupEffect.GoToLines(TransitDirection.FromCampus), effects.last())
            assertFalse(saved.isCaptured)

            model.onIntent(CommuteSetupIntent.OpenLines(TransitDirection.FromCampus))
            assertEquals(CommuteFixtures.CAMPUS_DEPARTURE_STOP_ID, model.state.value.linesStop?.id)
            assertEquals(listOf("003", "087", "088"), model.state.value.selectedLines)
            assertEquals(3, model.state.value.walkMinutes)
            model.onIntent(CommuteSetupIntent.SaveTapped)
            advanceUntilIdle()

            val plan = saved.captured
            assertEquals(CommuteFixtures.MY_STOP_ID, plan.toCampus?.stopId)
            assertEquals(listOf("087"), plan.toCampus?.lineNumbers)
            assertEquals(8, plan.toCampus?.walkMinutes)
            assertEquals(listOf("003", "087", "088"), plan.fromCampus?.lineNumbers)
            assertEquals(CommuteSetupEffect.Finished(plan, TransitDirection.ToCampus), effects.last())
            assertFalse(model.state.value.saving)
        }

    @Test
    fun editingPrefillsTheSavedLinesAndAFailedSaveStaysOnTheStep() =
        runTest {
            val save = mockk<SaveCommutePlanUseCase>()
            coEvery { save(any()) } returns Outcome.Err(TransitError.Connection)
            val model = model(save = save)
            val effects = mutableListOf<CommuteSetupEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.effects.toList(effects) }

            model.onIntent(CommuteSetupIntent.Begin(CommutePlan(toCampus = CommuteFixtures.plan.toCampus)))
            advanceUntilIdle()
            assertTrue(model.state.value.isEditing)
            assertEquals(CommuteFixtures.MY_STOP_ID, model.state.value.selectedStopId)

            model.onIntent(CommuteSetupIntent.OpenLines(TransitDirection.ToCampus))
            assertEquals(listOf("087", "088"), model.state.value.selectedLines)
            assertEquals(5, model.state.value.walkMinutes)

            model.onIntent(CommuteSetupIntent.SaveTapped)
            advanceUntilIdle()
            assertTrue(model.state.value.saveFailed)
            assertTrue(effects.none { it is CommuteSetupEffect.Finished })
        }

    @Test
    fun theLastDirectionCannotBeUnchecked() =
        runTest {
            val model = model()
            model.onIntent(CommuteSetupIntent.Begin(CommutePlan()))
            advanceUntilIdle()
            model.onIntent(CommuteSetupIntent.DirectionToggled(TransitDirection.ToCampus))
            assertEquals(listOf(TransitDirection.ToCampus), model.state.value.directions)
        }

    @Test
    fun searchIsAccentInsensitiveAndTheSavedStopLeadsTheList() =
        runTest {
            val model = model()
            model.onIntent(CommuteSetupIntent.Begin(CommuteFixtures.plan))
            advanceUntilIdle()
            assertEquals(CommuteFixtures.MY_STOP_ID, model.state.value.stops.first().id)
            model.onIntent(CommuteSetupIntent.QueryChanged("quiteria"))
            assertEquals(listOf("Av. Maria Quitéria, 3203"), model.state.value.stops.map { it.name })
        }

    private fun model(save: SaveCommutePlanUseCase = mockk()): CommuteSetupViewModel {
        val observePlan = mockk<ObserveCommutePlanUseCase>()
        every { observePlan() } returns flowOf(CommutePlan())
        val network = mockk<GetTransitNetworkUseCase>()
        coEvery { network() } returns Outcome.Ok(CommuteFixtures.network)
        val location = mockk<DeviceLocationClient>(relaxed = true)
        return CommuteSetupViewModel(
            observeCommutePlan = observePlan,
            getTransitNetwork = network,
            saveCommutePlan = save,
            removeCommutePlan = mockk<RemoveCommutePlanUseCase>(),
            locationClient = location,
            analytics = NoOpAnalytics,
        )
    }
}
