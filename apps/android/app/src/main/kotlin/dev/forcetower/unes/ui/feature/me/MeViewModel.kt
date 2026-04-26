package dev.forcetower.unes.ui.feature.me

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.disciplines.domain.usecase.CalculateOverallScoreUseCase
import dev.forcetower.melon.feature.me.domain.model.MeNextExam
import dev.forcetower.melon.feature.me.domain.model.MeProfile
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch

// "Eu" tab. Mirrors `MeViewModel` on iOS — one KMP flow drives the hero
// payload (identity + semester + per-semester credits + next exam) and a
// second flow carries the lifetime CR. Both feed a single mapped
// `ProfileIdentity` consumed by the screen.
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
}

internal sealed interface MeEffect : UiEffect

internal enum class LogoutStep { Idle, Confirming, Flashing, LoggedOut }

internal data class MeUiState(
    val profileRaw: MeProfile? = null,
    val overallScore: Double? = null,
    val logoutStep: LogoutStep = LogoutStep.Idle,
    // Captured at the start of the flash so the goodbye screen reads the
    // right name even after the profile flow has been wiped by `logout()`.
    val logoutFirstName: String = "Estudante",
) : UiState {
    // Null until the profile flow emits — the screen hides the hero in that
    // window rather than substitute fake fixture content. `MeFixtures.identity`
    // is preview-only.
    val identity: ProfileIdentity? = profileRaw?.let { mapIdentity(it, overallScore) }
}

@HiltViewModel
internal class MeViewModel @Inject constructor(
    observeMeProfile: ObserveMeProfileUseCase,
    overallScore: CalculateOverallScoreUseCase,
    private val sessionStore: SessionStore,
) : MviViewModel<MeUiState, MeIntent, MeEffect>(MeUiState()) {

    init {
        viewModelScope.launch {
            observeMeProfile().collect { value -> setState { copy(profileRaw = value) } }
        }
        viewModelScope.launch {
            overallScore(capSemesterId = null).collect { value ->
                setState { copy(overallScore = value) }
            }
        }
    }

    override fun onIntent(intent: MeIntent) {
        when (intent) {
            MeIntent.BeginLogout -> setState { copy(logoutStep = LogoutStep.Confirming) }
            MeIntent.CancelLogout -> setState { copy(logoutStep = LogoutStep.Idle) }
            is MeIntent.ConfirmLogout -> performLogout(intent.keepData)
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

private val PtBr: Locale = Locale.forLanguageTag("pt-BR")
private val ShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", PtBr)

private fun mapIdentity(raw: MeProfile, overallScore: Double?): ProfileIdentity {
    val canonical = raw.identity.userName.ifBlank { raw.identity.firstName }
    val first = raw.identity.firstName.ifBlank { canonical.substringBefore(' ') }
    val initial = first.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val semester = raw.semester
    return ProfileIdentity(
        name = canonical,
        firstName = first,
        course = raw.identity.courseName.orEmpty(),
        // Hardcoded today: every upstream is UEFS — see iOS MeViewModel.
        campus = "Universidade Estadual de Feira de Santana",
        enrollment = raw.identity.enrollmentNumber,
        username = raw.identity.username.orEmpty(),
        avatarInitial = initial,
        semester = semester?.code.orEmpty(),
        semesterWeek = semester?.currentWeek ?: 0,
        semesterTotalWeeks = semester?.totalWeeks ?: 0,
        progressPct = semester?.progressPercent ?: 0,
        // NaN is the "score not yet emitted" sentinel — the IdentityCard
        // renders "—" for it instead of a fake 0,0 / 8,5 value.
        cr = overallScore ?: Double.NaN,
        // Lifetime CR has no natural delta — leave blank so the up-arrow row
        // collapses (iOS does the same).
        crDelta = "",
        creditsDone = raw.enrollment.completedHours,
        creditsRequired = raw.enrollment.totalHours,
        semesterStart = formatSemesterStart(semester?.startDate),
        semesterEnd = formatSemesterEnd(semester?.endDate),
        finalExam = formatFinalExam(raw.nextExam),
    )
}

private fun formatSemesterStart(iso: String?): String {
    val date = parseDate(iso) ?: return ""
    return "início · ${ShortDateFormatter.format(date).replace(".", "")}"
}

private fun formatSemesterEnd(iso: String?): String {
    val date = parseDate(iso) ?: return ""
    return "fim · ${ShortDateFormatter.format(date).replace(".", "")}"
}

private fun formatFinalExam(exam: MeNextExam?): String {
    val date = parseDate(exam?.date) ?: return ""
    return "prova final · ${ShortDateFormatter.format(date).replace(".", "")}"
}

private fun parseDate(iso: String?): LocalDate? =
    iso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
