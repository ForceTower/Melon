package dev.forcetower.unes.ui.feature.me

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.campusevent.domain.usecase.ClearCampusEventUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.CalculateOverallScoreUseCase
import dev.forcetower.melon.feature.disciplines.domain.model.OverallScore
import dev.forcetower.melon.feature.disciplines.domain.model.ProgramScore
import dev.forcetower.melon.feature.me.domain.model.AcademicDocument
import dev.forcetower.melon.feature.me.domain.model.DocumentFetchError
import dev.forcetower.melon.feature.me.domain.model.MeProfile
import dev.forcetower.melon.feature.me.domain.usecase.FetchAcademicDocumentUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.melon.feature.me.domain.usecase.UpdateProfileUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncProfileUseCase
import dev.forcetower.unes.remote.FeatureFlags
import dev.forcetower.unes.remote.FeatureGates
import dev.forcetower.unes.firebase.PushRegistrar
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    // ── profile customization (dc `EuScreen` "Editar perfil" sheet) ──
    data object OpenEditProfile : MeIntent
    data object CloseEditProfile : MeIntent
    data class EditNameChanged(val value: String) : MeIntent
    // JPEG bytes produced by the circular crop step.
    data class EditPhotoPicked(val bytes: ByteArray) : MeIntent
    data object EditPhotoRemoved : MeIntent
    data object SaveProfile : MeIntent
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
    // Advances the Score stat to the next program. Only reachable when the
    // student has more than one.
    data object CycleScoreProgram : MeIntent
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

// What happens to the profile picture when the edit sheet is saved. `Keep`
// leaves the server photo alone; `Remove` deletes it; `Replace` uploads the
// freshly cropped bytes.
internal enum class PendingPhotoAction { Keep, Remove, Replace }

internal data class ProfileEditState(
    // Alternate-name draft — empty means "portal name in charge".
    val pendingName: String,
    val photoAction: PendingPhotoAction = PendingPhotoAction.Keep,
    // Cropped JPEG bytes staged for upload when `photoAction == Replace`.
    val pendingPhoto: ByteArray? = null,
    val saving: Boolean = false,
    val failed: Boolean = false,
)

// Transient confirmation pill after a successful save — same message split as
// the dc sheet's snackbar: photo-only changes narrate the photo, otherwise the
// name outcome wins.
internal sealed interface MeProfileToast {
    data class NameSaved(val firstName: String) : MeProfileToast
    data object NameRestored : MeProfileToast
    data object PhotoSaved : MeProfileToast
    data object PhotoRemoved : MeProfileToast
}

// dc `EuScreen` caps the field at 24 chars (the API allows 60) — enforced on
// both the TextField and the intent so paste can't sneak past it.
internal const val ProfileNameMaxLength = 24

internal data class MeUiState(
    val profileRaw: MeProfile? = null,
    val scoreRaw: OverallScore = OverallScore.Empty,
    // Which program the Score stat is showing. Null follows the program the
    // student is currently in; tapping the stat pins an index and cycles.
    val scoreProgramIndex: Int? = null,
    val gates: FeatureGates = FeatureGates(),
    val documentSheet: DocumentSheetState? = null,
    val editProfile: ProfileEditState? = null,
    val profileToast: MeProfileToast? = null,
    val logoutStep: LogoutStep = LogoutStep.Idle,
    // Captured at the start of the flash so the goodbye screen reads the
    // right name even after the profile flow has been wiped by `logout()`.
    val logoutFirstName: String = "Estudante",
) : UiState {
    // Null until the profile flow emits — the screen hides the hero in that
    // window rather than substitute fake fixture content. `MeFixtures.identity`
    // is preview-only.
    val identity: ProfileIdentity? = profileRaw?.let { mapIdentity(it, shownProgram, scoreRaw.isSplit) }

    // The pinned program if the student cycled to one, else the current one.
    val shownProgram: ProgramScore?
        get() = scoreProgramIndex?.let { scoreRaw.programs.getOrNull(it) } ?: scoreRaw.current
    val shortcuts: List<Shortcut> = MeFixtures.gridShortcuts(gates)
}

@HiltViewModel
internal class MeViewModel @Inject constructor(
    observeMeProfile: ObserveMeProfileUseCase,
    overallScore: CalculateOverallScoreUseCase,
    featureFlags: FeatureFlags,
    private val fetchDocument: FetchAcademicDocumentUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val syncProfile: SyncProfileUseCase,
    private val localDocuments: LocalDocumentStore,
    private val sessionStore: SessionStore,
    private val clearCampusEvent: ClearCampusEventUseCase,
    private val pushRegistrar: PushRegistrar,
    private val analytics: Analytics,
) : MviViewModel<MeUiState, MeIntent, MeEffect>(MeUiState()) {

    init {
        viewModelScope.launch {
            observeMeProfile().collect { value -> setState { copy(profileRaw = value) } }
        }
        viewModelScope.launch {
            overallScore().collect { value -> setState { copy(scoreRaw = value) } }
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
            MeIntent.OpenEditProfile -> openEditProfile()
            MeIntent.CloseEditProfile -> setState { copy(editProfile = null) }
            is MeIntent.EditNameChanged -> updateEditProfile {
                it.copy(pendingName = intent.value.take(ProfileNameMaxLength), failed = false)
            }
            is MeIntent.EditPhotoPicked -> updateEditProfile {
                it.copy(photoAction = PendingPhotoAction.Replace, pendingPhoto = intent.bytes, failed = false)
            }
            MeIntent.EditPhotoRemoved -> updateEditProfile {
                it.copy(photoAction = PendingPhotoAction.Remove, pendingPhoto = null, failed = false)
            }
            MeIntent.SaveProfile -> saveProfile()
            MeIntent.BeginLogout -> setState { copy(logoutStep = LogoutStep.Confirming) }
            MeIntent.CancelLogout -> setState { copy(logoutStep = LogoutStep.Idle) }
            MeIntent.ConfirmLogout -> performLogout()
            MeIntent.ResetLogout -> setState {
                copy(
                    logoutStep = LogoutStep.Idle,
                    profileRaw = null,
                    scoreRaw = OverallScore.Empty,
                    scoreProgramIndex = null,
                    documentSheet = null,
                )
            }
            MeIntent.CycleScoreProgram -> setState {
                val programs = scoreRaw.programs
                if (programs.size < 2) return@setState this
                val shown = programs.indexOf(shownProgram).coerceAtLeast(0)
                copy(scoreProgramIndex = (shown + 1) % programs.size)
            }
        }
    }

    // Fired from the composable for shortcuts that only bubble a hoisted nav
    // lambda (no ViewModel handler of their own to piggyback on).
    fun trackShortcutOpen(itemId: String) {
        analytics.selectContent(ContentTypes.SHORTCUT, itemId)
    }

    // ───────── Profile customization (Editar perfil) ─────────

    private var toastJob: Job? = null

    private fun openEditProfile() {
        val identity = currentState.identity ?: return
        analytics.selectContent(ContentTypes.SETTING, "edit_profile")
        // The field drafts the *alternate* name only — when the display name
        // is the portal name the field opens empty, matching the dc sheet.
        val nickname = identity.name.takeIf { it != identity.officialName }.orEmpty()
        setState { copy(editProfile = ProfileEditState(pendingName = nickname)) }
    }

    private fun saveProfile() {
        val sheet = currentState.editProfile ?: return
        if (sheet.saving) return
        val identity = currentState.identity ?: return

        val trimmed = sheet.pendingName.trim()
        val currentNickname = identity.name.takeIf { it != identity.officialName }.orEmpty()
        val nameChanged = trimmed != currentNickname
        val photoBytes = sheet.pendingPhoto
        val uploadsPhoto = sheet.photoAction == PendingPhotoAction.Replace && photoBytes != null
        // Removing when no server photo exists is a visual no-op — skip the call.
        val removesPhoto = sheet.photoAction == PendingPhotoAction.Remove && identity.avatarUrl != null

        if (!nameChanged && !uploadsPhoto && !removesPhoto) {
            setState { copy(editProfile = null) }
            return
        }

        setState { copy(editProfile = sheet.copy(saving = true, failed = false)) }
        viewModelScope.launch {
            val nameOk = !nameChanged ||
                updateProfile.updateName(trimmed.ifEmpty { null }) is Outcome.Ok
            val photoOk = when {
                !nameOk -> false
                uploadsPhoto -> updateProfile.updatePicture(photoBytes, "image/jpeg") is Outcome.Ok
                removesPhoto -> updateProfile.removePicture() is Outcome.Ok
                else -> true
            }
            if (!nameOk || !photoOk) {
                updateEditProfile { it.copy(saving = false, failed = true) }
                return@launch
            }
            analytics.selectContent(ContentTypes.SETTING, "edit_profile", mapOf("action" to "save"))
            // Re-pull the profile so the mirrored User row picks up what the
            // server actually stored (it normalizes a re-typed portal name to
            // null, and mints a fresh picture URL). The Room-backed flow then
            // re-emits and the hero — plus every other screen rendering the
            // name — updates on its own. A failed sync here is tolerable: the
            // server already saved, and the next sync tick converges.
            syncProfile()
            showToast(
                when {
                    !nameChanged -> {
                        if (removesPhoto) MeProfileToast.PhotoRemoved else MeProfileToast.PhotoSaved
                    }
                    trimmed.isNotEmpty() -> MeProfileToast.NameSaved(trimmed.substringBefore(' '))
                    else -> MeProfileToast.NameRestored
                },
            )
        }
    }

    private fun showToast(toast: MeProfileToast) {
        setState { copy(editProfile = null, profileToast = toast) }
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(ToastLifetimeMs)
            setState { copy(profileToast = null) }
        }
    }

    // Applies `transform` only while the edit sheet is open — a stale save
    // result must not resurrect a sheet the user already dismissed.
    private fun updateEditProfile(transform: (ProfileEditState) -> ProfileEditState) {
        setState {
            val sheet = editProfile
            if (sheet == null) this else copy(editProfile = transform(sheet))
        }
    }

    // ───────── Document sheet (Comprovante / Histórico) ─────────

    private fun openDocument(document: AcademicDocument) {
        analytics.selectContent(ContentTypes.SHORTCUT, document.shortcutItemId)
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
        analytics.selectContent(ContentTypes.DOCUMENT, sheet.document.kind, mapOf("action" to "fetch"))
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
        analytics.selectContent(ContentTypes.SETTING, "logout", mapOf("action" to "logout"))
        val firstName = currentState.identity?.firstName?.ifBlank { null } ?: "Estudante"
        setState { copy(logoutStep = LogoutStep.Flashing, logoutFirstName = firstName) }
        viewModelScope.launch {
            // Before logout() — the DELETEs need the session bearer.
            runCatching { pushRegistrar.unregisterAll() }
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
        // dc snackbar lifetime (2.6s).
        const val ToastLifetimeMs = 2600L
    }
}

// analytics itemId for the two shortcuts that route through the document
// sheet rather than straight navigation.
private val AcademicDocument.shortcutItemId: String
    get() = when (this) {
        AcademicDocument.EnrollmentCertificate -> "certificate"
        AcademicDocument.AcademicHistory -> "academic_history"
    }

// ───────── KMP → UI mapping ─────────

private val ShortDateFormatter: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

private fun mapIdentity(raw: MeProfile, score: ProgramScore?, isSplit: Boolean): ProfileIdentity {
    val canonical = raw.identity.userName.ifBlank { raw.identity.firstName }
    val first = raw.identity.firstName.ifBlank { canonical.substringBefore(' ') }
    val initial = first.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val semester = raw.semester
    return ProfileIdentity(
        name = canonical,
        firstName = first,
        officialName = raw.identity.officialName.ifBlank { canonical },
        avatarUrl = raw.identity.avatarUrl?.takeIf { it.isNotBlank() },
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
        crTrack = score?.track,
        crIsSplit = isSplit,
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
