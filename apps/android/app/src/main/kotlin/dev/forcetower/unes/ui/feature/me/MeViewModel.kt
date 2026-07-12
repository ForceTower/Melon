package dev.forcetower.unes.ui.feature.me

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.campusevent.domain.usecase.ClearCampusEventUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.CalculateOverallScoreUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.OverallScoreSummary
import dev.forcetower.melon.feature.me.domain.model.AcademicDocument
import dev.forcetower.melon.feature.me.domain.model.DocumentFetchError
import dev.forcetower.melon.feature.me.domain.model.MeProfile
import dev.forcetower.melon.feature.me.domain.usecase.FetchAcademicDocumentUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.unes.firebase.FeatureFlags
import dev.forcetower.unes.firebase.FeatureGates
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import dev.forcetower.unes.ui.feature.me.documents.LocalDocumentStore
import dev.forcetower.unes.ui.feature.me.documents.StoredAcademicDocument
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    data class OpenDocument(val document: AcademicDocument) : MeIntent
    data object CloseDocument : MeIntent
    // Fired by the download CTA, the refresh ghost button, and the retry
    // button alike — the stage decides what it means.
    data object RequestDocument : MeIntent
    data class CaptchaSolved(val token: String) : MeIntent
    data object CaptchaCanceled : MeIntent
    data object BeginLogout : MeIntent
    data object CancelLogout : MeIntent
    data object ConfirmLogout : MeIntent
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

// The document sheet's stage machine — a faithful port of iOS
// `MeDocumentFeature.State.Stage`.
internal sealed interface DocumentStage {
    // Nothing saved yet — just the download CTA.
    data object Intro : DocumentStage

    // The offline copy, offered straight away on open.
    data object Saved : DocumentStage

    // Remote config delivered a reCAPTCHA site key — solve it first.
    data object Captcha : DocumentStage

    data object Generating : DocumentStage

    // Refresh landed — the offline copy was just replaced.
    data object Fresh : DocumentStage

    // Refresh failed — showing the offline copy. The stamp is the server
    // copy's generation date when its fallback answered, or the local save
    // date when nothing did.
    data class Stale(val savedAtMs: Long) : DocumentStage

    // Nothing to show, and no saved copy to fall back on.
    data class Failed(val reason: DocumentFetchError) : DocumentStage
}

internal data class DocumentSheetState(
    val document: AcademicDocument,
    val stage: DocumentStage,
    val stored: StoredAcademicDocument? = null,
)

internal data class MeUiState(
    val profileRaw: MeProfile? = null,
    val scoreRaw: OverallScoreSummary? = null,
    val gates: FeatureGates = FeatureGates(),
    val documentSheet: DocumentSheetState? = null,
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
    private val fetchDocument: FetchAcademicDocumentUseCase,
    private val localDocuments: LocalDocumentStore,
    private val sessionStore: SessionStore,
    private val clearCampusEvent: ClearCampusEventUseCase,
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
            is MeIntent.OpenDocument -> openDocument(intent.document)
            MeIntent.CloseDocument -> setState { copy(documentSheet = null) }
            MeIntent.RequestDocument -> requestDocument()
            is MeIntent.CaptchaSolved -> startFetch(captchaToken = intent.token)
            MeIntent.CaptchaCanceled -> updateDocumentSheet { sheet ->
                sheet.copy(stage = if (sheet.stored == null) DocumentStage.Intro else DocumentStage.Saved)
            }
            MeIntent.BeginLogout -> setState { copy(logoutStep = LogoutStep.Confirming) }
            MeIntent.CancelLogout -> setState { copy(logoutStep = LogoutStep.Idle) }
            MeIntent.ConfirmLogout -> performLogout()
            MeIntent.ResetLogout -> setState {
                copy(logoutStep = LogoutStep.Idle, profileRaw = null, scoreRaw = null, documentSheet = null)
            }
        }
    }

    // ───────── Document sheet (Comprovante / Histórico) ─────────

    private fun openDocument(document: AcademicDocument) {
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) { localDocuments.load(document) }
            setState {
                copy(
                    documentSheet = DocumentSheetState(
                        document = document,
                        stage = if (stored == null) DocumentStage.Intro else DocumentStage.Saved,
                        stored = stored,
                    ),
                )
            }
        }
    }

    private fun requestDocument() {
        if (currentState.gates.documentCaptchaSiteKey.isNotEmpty()) {
            updateDocumentSheet { it.copy(stage = DocumentStage.Captcha) }
            return
        }
        startFetch(captchaToken = null)
    }

    private fun startFetch(captchaToken: String?) {
        val sheet = currentState.documentSheet ?: return
        updateDocumentSheet { it.copy(stage = DocumentStage.Generating) }
        viewModelScope.launch {
            when (val outcome = fetchDocument(sheet.document, captchaToken)) {
                is Outcome.Ok -> {
                    val fetched = outcome.value
                    val stored = withContext(Dispatchers.IO) {
                        runCatching { localDocuments.save(sheet.document, fetched.bytes) }.getOrNull()
                    }
                    updateDocumentSheet { current ->
                        val effective = stored ?: current.stored
                        when {
                            effective == null -> current.copy(
                                stage = DocumentStage.Failed(DocumentFetchError.Connection),
                            )
                            fetched.fresh -> current.copy(stage = DocumentStage.Fresh, stored = effective)
                            else -> current.copy(
                                // Server fell back to its newest stored copy —
                                // badge it with that copy's generation date.
                                stage = DocumentStage.Stale(
                                    savedAtMs = parseIsoMs(fetched.generatedAtIso) ?: effective.savedAtMs,
                                ),
                                stored = effective,
                            )
                        }
                    }
                }
                is Outcome.Err -> updateDocumentSheet { current ->
                    val stored = current.stored
                    if (stored != null) {
                        current.copy(stage = DocumentStage.Stale(savedAtMs = stored.savedAtMs))
                    } else {
                        current.copy(stage = DocumentStage.Failed(outcome.error))
                    }
                }
            }
        }
    }

    // Applies `transform` only while the sheet is still open for the same
    // document — a fetch landing after the user closed (or reopened another
    // kind) must not resurrect the old sheet.
    private fun updateDocumentSheet(transform: (DocumentSheetState) -> DocumentSheetState) {
        val document = currentState.documentSheet?.document ?: return
        setState {
            val sheet = documentSheet
            if (sheet == null || sheet.document != document) this
            else copy(documentSheet = transform(sheet))
        }
    }

    private fun performLogout() {
        val firstName = currentState.identity?.firstName?.ifBlank { null } ?: "Estudante"
        setState { copy(logoutStep = LogoutStep.Flashing, logoutFirstName = firstName) }
        viewModelScope.launch {
            runCatching { sessionStore.logout() }
            // User-scoped snapshot outside the DB teardown — iOS wipes it
            // with the mirror; here it lives in KeyValueStorage.
            runCatching { clearCampusEvent() }
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

// ISO8601 with fractional seconds (the backend's `createdAt` convention) →
// epoch millis; null when the stamp doesn't parse.
private fun parseIsoMs(iso: String): Long? =
    runCatching { Instant.parse(iso).toEpochMilli() }.getOrNull()
