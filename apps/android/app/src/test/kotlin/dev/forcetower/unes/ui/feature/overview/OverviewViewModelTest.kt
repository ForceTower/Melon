package dev.forcetower.unes.ui.feature.overview

import dev.forcetower.melon.core.analytics.NoOpAnalytics
import dev.forcetower.melon.core.common.AppClock
import dev.forcetower.melon.core.common.ForegroundSignal
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.TlsInterceptionMonitor
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.campusevent.domain.usecase.ObserveCampusEventUseCase
import dev.forcetower.melon.feature.me.domain.model.ReauthError
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ReauthenticateUpstreamUseCase
import dev.forcetower.melon.feature.me.domain.usecase.RefreshCredentialStatusUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveNextTestTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveNowClassUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveOverviewHeaderUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveTodayTimelineUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveTomorrowPreviewUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveUnreadMessagesTileUseCase
import dev.forcetower.unes.remote.FeatureFlags
import dev.forcetower.unes.remote.FeatureGates
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

// The view model runs an endless clock ticker, so the scheduler is driven by
// hand with `runCurrent` instead of `runTest`, which drains it to idle.
@OptIn(ExperimentalCoroutinesApi::class)
internal class OverviewViewModelTest {
    private val scheduler = TestCoroutineScheduler()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun aDismissedReauthDoesNotAnswerTheAttemptAfterIt() {
        val replies = mutableListOf<CompletableDeferred<Outcome<Unit, ReauthError>>>()
        val reauthenticate = mockk<ReauthenticateUpstreamUseCase>()
        coEvery { reauthenticate(any(), any()) } coAnswers {
            CompletableDeferred<Outcome<Unit, ReauthError>>().also(replies::add).await()
        }
        val model = model(reauthenticate)
        val effects = mutableListOf<OverviewEffect>()
        val collector = CoroutineScope(UnconfinedTestDispatcher(scheduler))
        collector.launch { model.effects.toList(effects) }
        scheduler.runCurrent()

        model.onIntent(OverviewIntent.ReauthTapped)
        model.onIntent(OverviewIntent.ReauthSubmitted("wrong"))
        scheduler.runCurrent()
        model.onIntent(OverviewIntent.ReauthDismissed)
        model.onIntent(OverviewIntent.ReauthTapped)
        model.onIntent(OverviewIntent.ReauthSubmitted("right"))
        scheduler.runCurrent()

        replies.first().complete(Outcome.Err(ReauthError.InvalidPassword))
        scheduler.runCurrent()
        assertTrue(model.state.value.reauthLoading)
        assertNull(model.state.value.reauthErrorRes)

        replies.last().complete(Outcome.Ok(Unit))
        scheduler.runCurrent()
        assertFalse(model.state.value.reauthLoading)
        assertEquals(1, effects.count { it == OverviewEffect.ReauthSucceeded })
        collector.cancel()
    }

    private fun model(reauthenticate: ReauthenticateUpstreamUseCase): OverviewViewModel {
        val observeHeader = mockk<ObserveOverviewHeaderUseCase>()
        every { observeHeader() } returns emptyFlow()
        val observeMeProfile = mockk<ObserveMeProfileUseCase>()
        every { observeMeProfile() } returns emptyFlow()
        val observeNowClass = mockk<ObserveNowClassUseCase>()
        every { observeNowClass() } returns emptyFlow()
        val observeToday = mockk<ObserveTodayTimelineUseCase>()
        every { observeToday() } returns emptyFlow()
        val observeTomorrow = mockk<ObserveTomorrowPreviewUseCase>()
        every { observeTomorrow() } returns emptyFlow()
        val observeMessagesTile = mockk<ObserveUnreadMessagesTileUseCase>()
        every { observeMessagesTile() } returns emptyFlow()
        val observeNextTestTile = mockk<ObserveNextTestTileUseCase>()
        every { observeNextTestTile() } returns emptyFlow()
        val observeCampusEvent = mockk<ObserveCampusEventUseCase>()
        every { observeCampusEvent() } returns emptyFlow()
        val featureFlags = mockk<FeatureFlags>()
        every { featureFlags.gates } returns MutableStateFlow(FeatureGates())
        val foregroundSignal = mockk<ForegroundSignal>()
        every { foregroundSignal.pulses } returns MutableSharedFlow()
        val sessionStore = mockk<SessionStore>()
        every { sessionStore.sessionInvalid } returns MutableStateFlow(false)
        every { sessionStore.credentialsInvalid } returns MutableStateFlow(true)
        every { sessionStore.upstreamUsername } returns MutableStateFlow("aluno")
        val refreshCredentialStatus = mockk<RefreshCredentialStatusUseCase>()
        coEvery { refreshCredentialStatus() } just runs
        return OverviewViewModel(
            appClock = AppClock(),
            observeHeader = observeHeader,
            observeMeProfile = observeMeProfile,
            observeNowClass = observeNowClass,
            observeToday = observeToday,
            observeTomorrow = observeTomorrow,
            observeMessagesTile = observeMessagesTile,
            observeNextTestTile = observeNextTestTile,
            observeCampusEvent = observeCampusEvent,
            refreshCampusEvent = mockk(),
            featureFlags = featureFlags,
            foregroundSignal = foregroundSignal,
            sessionStore = sessionStore,
            tlsInterceptionMonitor = TlsInterceptionMonitor(),
            refreshSession = mockk(),
            refreshCredentialStatus = refreshCredentialStatus,
            reauthenticateUpstream = reauthenticate,
            analytics = NoOpAnalytics,
        )
    }
}
