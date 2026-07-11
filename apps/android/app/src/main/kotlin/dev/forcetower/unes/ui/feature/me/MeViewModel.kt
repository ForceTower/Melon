package dev.forcetower.unes.ui.feature.me

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.disciplines.domain.usecase.CalculateOverallScoreUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.OverallScoreSummary
import dev.forcetower.melon.feature.me.domain.model.MeProfile
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.unes.firebase.FeatureFlags
import dev.forcetower.unes.firebase.FeatureGates
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch

// "Eu" tab. Mirrors `MeFeature` on iOS — one KMP flow drives the hero payload
// (identity + campus + attendance + semester), a second carries the lifetime
// CR with its delta, and the remote-config gates decide which shortcut tiles
// render. All three feed a single mapped `ProfileIdentity` + shortcut list
// consumed by the screen.
//
// The logout state machine lives here too: `Idle → Confirming → Flashing →
// LoggedOut`. When `LoggedOut` is reached the screen swaps in `LoggedOutView`;
// the CTA there bubbles up to the host nav, which replaces the back stack
// with `Welcome` — matching iOS, where `RootView` flips to `OnboardingFlow`
// on `AuthState.Unauthenticated`.
internal sealed interface MeIntent : UiIntent {
    data object BeginLogout : MeIntent
    data object CancelLogout : MeIntent
    data class ConfirmLogout(val keepData: Boolean) : MeIntent
    // Bounces the state machine back to Idle. The screen fires this when the
    // goodbye view's CTA is tapped — Hilt scopes this VM to the Activity, so
    // without an explicit reset the next time the Me tab mounts (after the
    // user signs back in) `logoutStep` is still `LoggedOut` and the goodbye
    // view sticks. iOS doesn't need this because `RootView` swaps the whole
    // `ConnectedView` destination, which destroys the VM.
    data object ResetLogout : MeIntent
}

internal sealed interface MeEffect : UiEffect

internal enum class LogoutStep { Idle, Confirming, Flashing, LoggedOut }

internal data class MeUiState(
    val profileRaw: MeProfile? = null,
    val scoreRaw: OverallScoreSummary? = null,
    val gates: FeatureGates = FeatureGates(),
    val logoutStep: LogoutStep = LogoutStep.Idle,
    // Captured at the start of the flash so the goodbye screen reads the
    // right name even after the profile flow has been wiped by `logout()`.
    val logoutFirstName: String = "Estudante",
) : UiState {
    // Null until the profile flow emits — the screen hides the hero in that
    // window rather than substitute fake fixture content. `MeFixtures.identity`
    // is preview-only.
    val identity: ProfileIdentity? = profileRaw?.let { mapIdentity(it, scoreRaw) }
    val shortcuts: List<Shortcut> = MeFixtures.gridShortcuts(gates)
    val materialsShortcut: Shortcut? = MeFixtures.materials.takeIf { gates.materials }
}

@HiltViewModel
internal class MeViewModel @Inject constructor(
    observeMeProfile: ObserveMeProfileUseCase,
    overallScore: CalculateOverallScoreUseCase,
    featureFlags: FeatureFlags,
    private val sessionStore: SessionStore,
) : MviViewModel<MeUiState, MeIntent, MeEffect>(MeUiState()) {

    init {
        viewModelScope.launch {
            observeMeProfile().collect { value -> setState { copy(profileRaw = value) } }
        }
        viewModelScope.launch {
            overallScore.summary().collect { value -> setState { copy(scoreRaw = value) } }
        }
        viewModelScope.launch {
            featureFlags.gates.collect { value -> setState { copy(gates = value) } }
        }
    }

    override fun onIntent(intent: MeIntent) {
        when (intent) {
            MeIntent.BeginLogout -> setState { copy(logoutStep = LogoutStep.Confirming) }
            MeIntent.CancelLogout -> setState { copy(logoutStep = LogoutStep.Idle) }
            is MeIntent.ConfirmLogout -> performLogout(intent.keepData)
            MeIntent.ResetLogout -> setState {
                copy(logoutStep = LogoutStep.Idle, profileRaw = null, scoreRaw = null)
            }
        }
    }

    private fun performLogout(@Suppress("UNUSED_PARAMETER") keepData: Boolean) {
        // `keepData` is captured for a future SessionStore branch that keeps
        // local prefs (themes, reminders) on logout; the KMP API doesn't take
        // it yet — match iOS, which threads it through to a no-op as well.
        val firstName = currentState.identity?.firstName?.ifBlank { null } ?: "Estudante"
        setState { copy(logoutStep = LogoutStep.Flashing, logoutFirstName = firstName) }
        viewModelScope.launch {
            runCatching { sessionStore.logout() }
            // Match iOS pacing: ~0.9s flash before the goodbye view animates in.
            kotlinx.coroutines.delay(LogoutFlashMs)
            setState { copy(logoutStep = LogoutStep.LoggedOut) }
        }
    }

    private companion object {
        const val LogoutFlashMs = 900L
    }
}

// ───────── KMP → UI mapping ─────────

private val ShortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

private fun mapIdentity(raw: MeProfile, score: OverallScoreSummary?): ProfileIdentity {
    val canonical = raw.identity.userName.ifBlank { raw.identity.firstName }
    val first = raw.identity.firstName.ifBlank { canonical.substringBefore(' ') }
    val initial = first.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val semester = raw.semester
    return ProfileIdentity(
        name = canonical,
        firstName = first,
        course = raw.identity.courseName.orEmpty(),
        campusLabel = raw.campus,
        enrollment = raw.identity.enrollmentNumber,
        username = raw.identity.username.orEmpty(),
        avatarInitial = initial,
        semesterWeek = semester?.currentWeek ?: 0,
        semesterTotalWeeks = semester?.totalWeeks ?: 0,
        progressPct = semester?.progressPercent ?: 0,
        cr = score?.value,
        crDelta = score?.delta,
        attendancePercent = raw.attendancePercent,
        semesterOrdinal = raw.semesterOrdinal,
        semesterStart = formatShortDate(semester?.startDate),
        semesterEnd = formatShortDate(semester?.endDate),
    )
}

// "2026-02-18" → "18 fev" (month abbreviation dot stripped, matching the dc
// footer labels).
private fun formatShortDate(iso: String?): String {
    val date = iso?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return ""
    return ShortDateFormatter.format(date).replace(".", "")
}
