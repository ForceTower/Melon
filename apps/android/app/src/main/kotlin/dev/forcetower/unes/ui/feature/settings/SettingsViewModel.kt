package dev.forcetower.unes.ui.feature.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.me.domain.usecase.ObserveCurrentCredentialsUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveLastSyncUseCase
import dev.forcetower.melon.feature.settings.domain.usecase.ObserveSettingsUseCase
import dev.forcetower.melon.feature.settings.domain.usecase.UpdateSettingsUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock

// Drives `SettingsScreen`. Mirrors iOS `SettingsViewModel`: subscribe to the
// credentials, last-sync, and settings flows; tick a 30s clock so the relative
// "há N min" stamp refreshes without forcing KMP flows to re-emit. Each
// mutation writes to local state optimistically, then forwards the patch to
// `UpdateSettingsUseCase`; the resulting flow emission overwrites the local
// value with the canonical server value (no-op for booleans, defensive for
// the spoiler integer).
internal sealed interface SettingsIntent : UiIntent {
    data class SetSpoiler(val value: SpoilerMode) : SettingsIntent
    data class SetToggle(val toggle: NotifToggle, val value: Boolean) : SettingsIntent
}

internal sealed interface SettingsEffect : UiEffect

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    observeCredentials: ObserveCurrentCredentialsUseCase,
    observeLastSync: ObserveLastSyncUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
) : MviViewModel<SettingsUiState, SettingsIntent, SettingsEffect>(SettingsUiState()) {

    init {
        viewModelScope.launch {
            observeCredentials().collect { creds ->
                setState { copy(username = creds?.username, password = creds?.password) }
            }
        }
        viewModelScope.launch {
            observeLastSync().collect { iso -> setState { copy(lastSyncIso = iso) } }
        }
        viewModelScope.launch {
            observeSettings().collect { snapshot ->
                if (snapshot != null) setState { applySnapshot(snapshot) }
            }
        }
        // Drives the relative "há N min" stamp. Same 30s cadence as
        // `OverviewViewModel` and iOS `runClockTicker`.
        viewModelScope.launch {
            while (isActive) {
                setState { copy(nowEpochSeconds = Clock.System.now().epochSeconds) }
                delay(CLOCK_TICK_MS)
            }
        }
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetSpoiler -> setSpoiler(intent.value)
            is SettingsIntent.SetToggle -> setToggle(intent.toggle, intent.value)
        }
    }

    private fun setSpoiler(value: SpoilerMode) {
        setState { copy(spoiler = value) }
        viewModelScope.launch {
            val outcome = updateSettings(gradeSpoiler = value.serverInt)
            if (outcome is Outcome.Err) {
                // Local mirror keeps the optimistic value; the next profile
                // sync will re-emit and reconcile if the server rejected it.
            }
        }
    }

    private fun setToggle(toggle: NotifToggle, value: Boolean) {
        setState { applyToggle(toggle, value) }
        viewModelScope.launch {
            val outcome = when (toggle) {
                NotifToggle.MsgBroadcast -> updateSettings(notifMsgBroadcast = value)
                NotifToggle.MsgClass -> updateSettings(notifMsgClass = value)
                NotifToggle.MsgDirect -> updateSettings(notifMsgDirect = value)
                NotifToggle.GradePosted -> updateSettings(notifGradePosted = value)
                NotifToggle.GradeChanged -> updateSettings(notifGradeChanged = value)
                NotifToggle.GradeDateChanged -> updateSettings(notifGradeDateChanged = value)
                NotifToggle.ClassLocation -> updateSettings(notifClassLocation = value)
                NotifToggle.ClassMaterial -> updateSettings(notifClassMaterial = value)
                NotifToggle.ClassSubject -> updateSettings(notifClassSubject = value)
            }
            if (outcome is Outcome.Err) {
                // See note in `setSpoiler`.
            }
        }
    }

    private companion object {
        const val CLOCK_TICK_MS = 30_000L
    }
}

private fun SettingsUiState.applyToggle(toggle: NotifToggle, value: Boolean): SettingsUiState =
    when (toggle) {
        NotifToggle.MsgBroadcast -> copy(notifMsgBroadcast = value)
        NotifToggle.MsgClass -> copy(notifMsgClass = value)
        NotifToggle.MsgDirect -> copy(notifMsgDirect = value)
        NotifToggle.GradePosted -> copy(notifGradePosted = value)
        NotifToggle.GradeChanged -> copy(notifGradeChanged = value)
        NotifToggle.GradeDateChanged -> copy(notifGradeDateChanged = value)
        NotifToggle.ClassLocation -> copy(notifClassLocation = value)
        NotifToggle.ClassMaterial -> copy(notifClassMaterial = value)
        NotifToggle.ClassSubject -> copy(notifClassSubject = value)
    }
