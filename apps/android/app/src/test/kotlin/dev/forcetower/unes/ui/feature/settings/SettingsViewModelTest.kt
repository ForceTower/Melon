package dev.forcetower.unes.ui.feature.settings

import androidx.lifecycle.viewModelScope
import dev.forcetower.melon.core.analytics.NoOpAnalytics
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.auth.domain.usecase.ListPasskeysUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveCurrentCredentialsUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.melon.feature.settings.domain.model.UserSettings
import dev.forcetower.melon.feature.settings.domain.repository.SettingsError
import dev.forcetower.melon.feature.settings.domain.usecase.ObserveSettingsUseCase
import dev.forcetower.melon.feature.settings.domain.usecase.UpdateSettingsUseCase
import dev.forcetower.unes.reminders.EvaluationReminderPreferenceStore
import dev.forcetower.unes.remote.FeatureFlags
import dev.forcetower.unes.remote.FeatureGates
import dev.forcetower.unes.theme.ThemePreferenceStore
import dev.forcetower.unes.ui.feature.commute.CommutePreferenceStore
import dev.forcetower.unes.ui.feature.schedule.SchedulePreferenceStore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

// The repository rolls an unconfirmed save back, so the switch flips back on
// its own; the student must be told why.
@OptIn(ExperimentalCoroutinesApi::class)
internal class SettingsViewModelTest {
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
    fun toggleTheServerNeverSavedIsReported() =
        runTest {
            val updateSettings = mockk<UpdateSettingsUseCase>()
            coEvery { updateSettings(notifGradePosted = false) } returns Outcome.Err(SettingsError.NoConnection)

            val effects = effectsOf(model(updateSettings), SettingsIntent.SetToggle(NotifToggle.GradePosted, false))

            assertEquals(listOf<SettingsEffect>(SettingsEffect.SaveFailed), effects)
        }

    @Test
    fun spoilerTheServerRejectedIsReported() =
        runTest {
            val spoiler = SpoilerMode.entries.last()
            val updateSettings = mockk<UpdateSettingsUseCase>()
            coEvery { updateSettings(gradeSpoiler = spoiler.serverInt) } returns Outcome.Err(SettingsError.Server(null))

            val effects = effectsOf(model(updateSettings), SettingsIntent.SetSpoiler(spoiler))

            assertEquals(listOf<SettingsEffect>(SettingsEffect.SaveFailed), effects)
        }

    @Test
    fun savedToggleReportsNothing() =
        runTest {
            val updateSettings = mockk<UpdateSettingsUseCase>()
            coEvery { updateSettings(notifGradePosted = false) } returns Outcome.Ok(saved)

            val effects = effectsOf(model(updateSettings), SettingsIntent.SetToggle(NotifToggle.GradePosted, false))

            assertEquals(emptyList(), effects)
        }

    // The preview clock ticks forever on the shared test scheduler, which
    // runTest drains on exit — so the model's scope is cancelled first.
    private fun TestScope.effectsOf(
        model: SettingsViewModel,
        intent: SettingsIntent,
    ): List<SettingsEffect> {
        val effects = mutableListOf<SettingsEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.effects.toList(effects) }
        try {
            runCurrent()
            model.onIntent(intent)
            runCurrent()
        } finally {
            model.viewModelScope.cancel()
        }
        return effects
    }

    private val saved = UserSettings(
        gradeSpoiler = 0,
        notifMsgBroadcast = true,
        notifMsgClass = true,
        notifMsgDirect = true,
        notifGradePosted = false,
        notifGradeChanged = true,
        notifGradeDateChanged = true,
        notifClassLocation = true,
        notifClassMaterial = true,
        notifClassSubject = true,
    )

    private fun model(updateSettings: UpdateSettingsUseCase): SettingsViewModel {
        val observeCredentials = mockk<ObserveCurrentCredentialsUseCase>()
        every { observeCredentials() } returns emptyFlow()
        val observeMeProfile = mockk<ObserveMeProfileUseCase>()
        every { observeMeProfile() } returns emptyFlow()
        val observeSettings = mockk<ObserveSettingsUseCase>()
        every { observeSettings() } returns emptyFlow()
        val listPasskeys = mockk<ListPasskeysUseCase>()
        coEvery { listPasskeys() } returns Outcome.Ok(emptyList())
        val themePreferences = mockk<ThemePreferenceStore>()
        every { themePreferences.mode } returns emptyFlow()
        val schedulePreferences = mockk<SchedulePreferenceStore>()
        every { schedulePreferences.gridEnabled } returns emptyFlow()
        val reminderPreferences = mockk<EvaluationReminderPreferenceStore>()
        every { reminderPreferences.enabled } returns emptyFlow()
        val commutePreferences = mockk<CommutePreferenceStore>()
        every { commutePreferences.onHome } returns emptyFlow()
        val featureFlags = mockk<FeatureFlags>()
        every { featureFlags.gates } returns MutableStateFlow(FeatureGates())
        return SettingsViewModel(
            observeCredentials = observeCredentials,
            observeMeProfile = observeMeProfile,
            observeSettings = observeSettings,
            updateSettings = updateSettings,
            listPasskeys = listPasskeys,
            themePreferences = themePreferences,
            schedulePreferences = schedulePreferences,
            reminderPreferences = reminderPreferences,
            commutePreferences = commutePreferences,
            featureFlags = featureFlags,
            analytics = NoOpAnalytics,
        )
    }
}
