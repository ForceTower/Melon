package dev.forcetower.unes.ui.feature.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.auth.domain.usecase.ListPasskeysUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveCurrentCredentialsUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.melon.feature.settings.domain.usecase.ObserveSettingsUseCase
import dev.forcetower.melon.feature.settings.domain.usecase.UpdateSettingsUseCase
import dev.forcetower.unes.firebase.FeatureFlags
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.reminders.EvaluationReminderPreferenceStore
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
    data class SetEvaluationReminders(val value: Boolean) : SettingsIntent
}

internal sealed interface SettingsEffect : UiEffect

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    observeCredentials: ObserveCurrentCredentialsUseCase,
    observeMeProfile: ObserveMeProfileUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val listPasskeys: ListPasskeysUseCase,
    private val themePreferences: ThemePreferenceStore,
    private val reminderPreferences: EvaluationReminderPreferenceStore,
    featureFlags: FeatureFlags,
    private val analytics: Analytics,
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
            reminderPreferences.enabled.collect { enabled ->
                setState { copy(evaluationRemindersEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            featureFlags.gates.collect { gates ->
                setState { copy(evaluationRemindersAvailable = gates.evaluationReminders) }
            }
        }
        viewModelScope.launch {
            observeSettings().collect { snapshot ->
                if (snapshot != null) setState { applySnapshot(snapshot) }
            }
        }
        refreshPasskeys()
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
            is SettingsIntent.SetEvaluationReminders -> setEvaluationReminders(intent.value)
        }
    }

    // Refetched on entry and on every foreground return so the "Chaves de
    // acesso" row's count stays honest after the manager screen adds or
    // revokes a key. A failed fetch keeps the last known count.
    fun refreshPasskeys() {
        viewModelScope.launch {
            val outcome = listPasskeys()
            if (outcome is Outcome.Ok) setState { copy(passkeyCount = outcome.value.size) }
        }
    }

    fun trackCredentialReveal() {
        analytics.selectContent(ContentTypes.SETTING, "credentials", mapOf("action" to "reveal"))
    }

    private fun setTheme(value: ThemeMode) {
        analytics.selectContent(
            ContentTypes.SETTING,
            "theme",
            mapOf("action" to "select", "value" to value.name.lowercase()),
        )
        setState { copy(themeMode = value) }
        viewModelScope.launch { themePreferences.set(value) }
    }

    // Device-local, no PATCH — the app-scope scheduler collects this store
    // and re-arms (or cancels) the alarm as soon as the write lands.
    private fun setEvaluationReminders(value: Boolean) {
        analytics.selectContent(
            ContentTypes.SETTING,
            "evaluation_reminders",
            mapOf("action" to "toggle", "value" to value),
        )
        setState { copy(evaluationRemindersEnabled = value) }
        viewModelScope.launch { reminderPreferences.set(value) }
    }

    private fun setSpoiler(value: SpoilerMode) {
        analytics.selectContent(
            ContentTypes.SETTING,
            "grade_spoiler",
            mapOf("action" to "select", "value" to value.name.lowercase()),
        )
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
        analytics.selectContent(
            ContentTypes.SETTING,
            toggle.analyticsKey,
            mapOf("action" to "toggle", "value" to value),
        )
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

private val NotifToggle.analyticsKey: String
    get() = when (this) {
        NotifToggle.MsgBroadcast -> "notif_msg_broadcast"
        NotifToggle.MsgClass -> "notif_msg_class"
        NotifToggle.MsgDirect -> "notif_msg_direct"
        NotifToggle.GradePosted -> "notif_grade_posted"
        NotifToggle.GradeChanged -> "notif_grade_changed"
        NotifToggle.GradeDateChanged -> "notif_grade_date_changed"
        NotifToggle.ClassLocation -> "notif_class_location"
        NotifToggle.ClassMaterial -> "notif_class_material"
        NotifToggle.ClassSubject -> "notif_class_subject"
    }
