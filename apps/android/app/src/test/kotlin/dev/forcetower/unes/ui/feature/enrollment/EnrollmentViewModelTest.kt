package dev.forcetower.unes.ui.feature.enrollment

import dev.forcetower.melon.core.analytics.NoOpAnalytics
import dev.forcetower.melon.core.common.AppClock
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentAvailability
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentDiscipline
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentOffers
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentPrerequisite
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSelection
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSlot
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindow
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindowState
import dev.forcetower.melon.feature.enrollment.domain.repository.EnrollmentError
import dev.forcetower.melon.feature.enrollment.domain.usecase.GetEnrollmentOffersUseCase
import dev.forcetower.melon.feature.enrollment.domain.usecase.GetEnrollmentWindowUseCase
import dev.forcetower.melon.feature.enrollment.domain.usecase.SubmitEnrollmentUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.unes.ui.feature.enrollment.EnrollmentTestFixtures.discipline
import dev.forcetower.unes.ui.feature.enrollment.EnrollmentTestFixtures.section
import dev.forcetower.unes.ui.feature.enrollment.EnrollmentTestFixtures.window
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
internal class EnrollmentViewModelTest {
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
    fun crossingDeadlineOnReviewRefreshesTheClockAndNeverCallsSubmit() =
        runTest {
            val clock = MutableClock(Instant.parse("2026-10-03T02:59:59Z"))
            val attempts = mutableListOf<List<EnrollmentSelection>>()
            val model = model(clock = clock) {
                attempts += it
                Outcome.Ok(Unit)
            }
            model.onIntent(EnrollmentIntent.Enter)
            advanceUntilIdle()
            assertTrue(model.state.value.canSubmit)

            clock.instant = Instant.parse("2026-10-03T03:00:00.001Z")
            model.onIntent(EnrollmentIntent.Submit)
            advanceUntilIdle()

            assertTrue(attempts.isEmpty())
            assertEquals(listOf(EnrollmentBlocker.DeadlinePassed), model.state.value.blockers)
            assertFalse(model.state.value.submitting)
        }

    @Test
    fun conflictingAndFullSectionsWithoutAQueueDoNotChangeTheProposal() =
        runTest {
            val catalogue = listOf(
                discipline(1, section(11, EnrollmentSlot(1, "08:00", "10:00")).copy(selected = true)),
                discipline(2, section(21, EnrollmentSlot(1, "09:00", "11:00"))),
                discipline(3, section(31).copy(vacancies = 0)),
            )
            val model = model(catalogue = catalogue, window = window().copy(useQueue = false))
            model.onIntent(EnrollmentIntent.Enter)
            advanceUntilIdle()

            model.onIntent(EnrollmentIntent.SectionTapped(2, 21))
            model.onIntent(EnrollmentIntent.SectionTapped(3, 31))

            assertEquals(listOf(EnrollmentSelection(11, false, false)), model.state.value.selections)
        }

    @Test
    fun failedSubmissionKeepsTheEditedCompleteProposalForRetryAndSuccessClosesIt() =
        runTest {
            val catalogue = listOf(
                discipline(1, section(11).copy(selected = true), section(12)),
                discipline(2, section(21).copy(selected = true)),
                discipline(3, section(31).copy(selected = true)),
                discipline(4, section(41).copy(vacancies = 0)).copy(
                    prerequisites = listOf(EnrollmentPrerequisite("SYN100", "Fundamentos sintéticos", false)),
                ),
            )
            val attempts = mutableListOf<List<EnrollmentSelection>>()
            val model = model(catalogue = catalogue) {
                attempts += it
                if (attempts.size == 1) Outcome.Err(EnrollmentError.NoConnection) else Outcome.Ok(Unit)
            }
            model.onIntent(EnrollmentIntent.Enter)
            advanceUntilIdle()
            model.onIntent(EnrollmentIntent.SectionTapped(1, 12))
            model.onIntent(EnrollmentIntent.AllowsOtherChanged(1, true))
            model.onIntent(EnrollmentIntent.RemovePick(2))
            model.onIntent(EnrollmentIntent.SectionTapped(4, 41))
            model.onIntent(EnrollmentIntent.Submit)
            advanceUntilIdle()

            val desired = listOf(
                EnrollmentSelection(12, true, false),
                EnrollmentSelection(31, false, false),
                EnrollmentSelection(41, false, true),
            )
            assertEquals(listOf(desired), attempts)
            assertEquals(desired, model.state.value.selections)
            assertEquals(EnrollmentError.NoConnection, model.state.value.submitError)
            assertTrue(model.state.value.canSubmit)

            model.onIntent(EnrollmentIntent.DismissSubmitError)
            model.onIntent(EnrollmentIntent.Submit)
            advanceUntilIdle()

            assertEquals(listOf(desired, desired), attempts)
            assertNull(model.state.value.submitError)
            assertEquals(EnrollmentWindowState.Closed, model.state.value.window?.state)
            assertTrue(model.state.value.isReadonly)
            assertEquals(EnrollmentEffect.Submitted, model.effects.first())
        }

    @Test
    fun repeatedSubmitWhileInFlightDoesNotSendTheTransactionTwice() =
        runTest {
            val result = CompletableDeferred<Outcome<Unit, EnrollmentError>>()
            var attempts = 0
            val model = model {
                attempts++
                result.await()
            }
            model.onIntent(EnrollmentIntent.Enter)
            advanceUntilIdle()
            model.onIntent(EnrollmentIntent.Submit)
            advanceUntilIdle()
            assertTrue(model.state.value.submitting)
            model.onIntent(EnrollmentIntent.Submit)
            advanceUntilIdle()
            assertEquals(1, attempts)

            result.complete(Outcome.Ok(Unit))
            advanceUntilIdle()
            assertFalse(model.state.value.submitting)
            assertEquals(EnrollmentEffect.Submitted, model.effects.first())
        }

    private fun model(
        catalogue: List<EnrollmentDiscipline> = listOf(discipline(1, section(11).copy(selected = true))),
        window: EnrollmentWindow = window(),
        clock: Clock = MutableClock(Instant.parse("2026-10-02T12:00:00Z")),
        submit: suspend (List<EnrollmentSelection>) -> Outcome<Unit, EnrollmentError> = { Outcome.Ok(Unit) },
    ): EnrollmentViewModel {
        val getWindow = mockk<GetEnrollmentWindowUseCase>()
        val getOffers = mockk<GetEnrollmentOffersUseCase>()
        val submitEnrollment = mockk<SubmitEnrollmentUseCase>()
        val observeMeProfile = mockk<ObserveMeProfileUseCase>()
        coEvery { getWindow() } returns Outcome.Ok(EnrollmentAvailability(true, window))
        coEvery { getOffers() } returns Outcome.Ok(EnrollmentOffers(catalogue))
        coEvery { submitEnrollment(any()) } coAnswers { submit(firstArg()) }
        every { observeMeProfile() } returns emptyFlow()
        return EnrollmentViewModel(
            getWindow = getWindow,
            getOffers = getOffers,
            submitEnrollment = submitEnrollment,
            analytics = NoOpAnalytics,
            observeMeProfile = observeMeProfile,
            appClock = AppClock(clock),
        )
    }
}

private class MutableClock(
    var instant: Instant,
) : Clock {
    override fun now() = instant
}
