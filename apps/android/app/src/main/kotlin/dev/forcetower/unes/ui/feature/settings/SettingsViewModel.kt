package dev.forcetower.unes.ui.feature.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.me.domain.usecase.ObserveCurrentCredentialsUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.melon.feature.settings.domain.usecase.ObserveSettingsUseCase
import dev.forcetower.melon.feature.settings.domain.usecase.UpdateSettingsUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.theme.ThemeMode
import dev.forcetower.unes.theme.ThemePreferenceStore
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock

// Drives `SettingsScreen` (dc `UNES Configurações - Android`). Subscribes to
// the profile identity (vault name/avatar), the credentials flow, the theme
// preference, and the user-settings flow; ticks a minute clock so the
// lock-screen preview stamp stays honest. Each server-backed mutation writes
// to local state optimistically, then forwards the patch to
// `UpdateSettingsUseCase`; the resulting flow emission overwrites the local
// value with the canonical server value. The theme mode is device-local and
// goes to DataStore instead.
internal sealed interface SettingsIntent : UiIntent {
    data class SetTheme(val value: ThemeMode) : SettingsIntent
    data class SetSpoiler(val value: SpoilerMode) : SettingsIntent
    data class SetToggle(val toggle: NotifToggle, val value: Boolean) : SettingsIntent
}

internal sealed interface SettingsEffect : UiEffect

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    observeCredentials: ObserveCurrentCredentialsUseCase,
    observeMeProfile: ObserveMeProfileUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val themePreferences: ThemePreferenceStore,
) : MviViewModel<SettingsUiState, SettingsIntent, SettingsEffect>(SettingsUiState()) {

    init {
        viewModelScope.launch {
            observeCredentials().collect { creds ->
                setState { copy(username = creds?.username, password = creds?.password) }
            }
        }
        viewModelScope.launch {
            observeMeProfile().collect { profile ->
                val name = profile.identity.userName
                val first = profile.identity.firstName.ifBlank { name }
                setState {
                    copy(
                        displayName = name,
                        avatarInitial = first.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        campusLabel = profile.campus,
                    )
                }
            }
        }
        viewModelScope.launch {
            themePreferences.mode.collect { mode -> setState { copy(themeMode = mode) } }
        }
        viewModelScope.launch {
            observeSettings().collect { snapshot ->
                if (snapshot != null) setState { applySnapshot(snapshot) }
            }
        }
        // Drives the lock-screen preview clock stamp.
        viewModelScope.launch {
            while (isActive) {
                setState { copy(nowEpochSeconds = Clock.System.now().epochSeconds) }
                delay(CLOCK_TICK_MS)
            }
        }
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetTheme -> setTheme(intent.value)
            is SettingsIntent.SetSpoiler -> setSpoiler(intent.value)
            is SettingsIntent.SetToggle -> setToggle(intent.toggle, intent.value)
        }
    }

    private fun setTheme(value: ThemeMode) {
        setState { copy(themeMode = value) }
        viewModelScope.launch { themePreferences.set(value) }
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
