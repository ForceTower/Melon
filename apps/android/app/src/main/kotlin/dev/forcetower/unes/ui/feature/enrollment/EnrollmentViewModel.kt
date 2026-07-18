package dev.forcetower.unes.ui.feature.enrollment

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindowState
import dev.forcetower.melon.feature.enrollment.domain.usecase.GetEnrollmentOffersUseCase
import dev.forcetower.melon.feature.enrollment.domain.usecase.GetEnrollmentWindowUseCase
import dev.forcetower.melon.feature.enrollment.domain.usecase.SubmitEnrollmentUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.unes.mvi.MviViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// One activity-scoped ViewModel for the whole matrícula stack (status →
// offers → discipline → timetable → review → success). Nav3 entries all
// resolve the same instance — the Paradoxo/Messages trick — so the in-memory
// session (picks + catalogue) survives pushes without serialized payloads.
// Data is live/uncached: vacancy counts shift by the second during a window.
@HiltViewModel
internal class EnrollmentViewModel @Inject constructor(
    private val getWindow: GetEnrollmentWindowUseCase,
    private val getOffers: GetEnrollmentOffersUseCase,
    private val submitEnrollment: SubmitEnrollmentUseCase,
    private val analytics: Analytics,
    observeMeProfile: ObserveMeProfileUseCase,
) : MviViewModel<EnrollmentUiState, EnrollmentIntent, EnrollmentEffect>(EnrollmentUiState()) {

    private var loadJob: Job? = null
    private var submitJob: Job? = null

    init {
        // Identity strip garnish on the status header; failures are ignored.
        viewModelScope.launch {
            observeMeProfile().collect { profile ->
                setState {
                    copy(
                        studentName = profile.identity.userName.ifBlank { profile.identity.firstName },
                        courseName = profile.identity.courseName,
                        semesterOrdinal = profile.semesterOrdinal,
                    )
                }
            }
        }
    }

    override fun onIntent(intent: EnrollmentIntent) {
        when (intent) {
            EnrollmentIntent.Enter -> enter()
            EnrollmentIntent.Retry -> load(initial = true)
            is EnrollmentIntent.QueryChanged -> setState { copy(query = intent.query) }
            is EnrollmentIntent.FilterChanged -> setState { copy(filter = intent.filter) }
            is EnrollmentIntent.SectionTapped -> sectionTapped(intent.disciplineId, intent.sectionId)
            is EnrollmentIntent.RemovePick -> {
                if (currentState.canEdit) {
                    val sectionId = currentState.pickFor(intent.disciplineId)?.sectionId
                    setState { copy(picks = picks.filter { it.disciplineId != intent.disciplineId }) }
                    if (sectionId != null) {
                        analytics.selectContent(
                            contentType = ContentTypes.OFFER,
                            itemId = sectionId.toString(),
                            properties = mapOf("action" to "remove"),
                        )
                    }
                }
            }
            is EnrollmentIntent.AllowsOtherChanged -> {
                if (currentState.canEdit) {
                    setState {
                        copy(
                            picks = picks.map {
                                if (it.disciplineId == intent.disciplineId) it.copy(allowsOther = intent.value) else it
                            },
                        )
                    }
                }
            }
            EnrollmentIntent.Reopen -> setState { copy(reopened = true) }
            EnrollmentIntent.Submit -> submit()
            EnrollmentIntent.DismissSubmitError -> setState { copy(submitError = null) }
        }
    }

    private fun enter() {
        setState { copy(referenceNowMillis = System.currentTimeMillis()) }
        when (currentState.phase) {
            EnrollmentPhase.Loading -> Unit
            EnrollmentPhase.Idle, EnrollmentPhase.Failed -> load(initial = true)
            // Re-mounts of the status hub (back-pops, later re-entries) pull
            // fresh vacancy counts without disturbing picks or the UI.
            EnrollmentPhase.Loaded -> load(initial = false)
        }
    }

    // Window first (the cheap gate), offers only when a window exists.
    // `initial` shows the full-screen spinner, resets the session and
    // preseeds picks from the saved proposal; silent refreshes keep picks —
    // vanished sections drop out via `resolvedPicks`.
    private fun load(initial: Boolean) {
        loadJob?.cancel()
        if (initial) {
            setState {
                copy(
                    phase = EnrollmentPhase.Loading,
                    error = null,
                    offersFailed = false,
                    picks = emptyList(),
                    submitError = null,
                    reopened = false,
                    query = "",
                    filter = EnrollmentFilter.All,
                )
            }
        }
        loadJob = viewModelScope.launch {
            when (val availability = getWindow()) {
                is Outcome.Err -> {
                    if (initial) setState { copy(phase = EnrollmentPhase.Failed, error = availability.error) }
                }
                is Outcome.Ok -> {
                    val window = availability.value.window.takeIf { availability.value.available }
                    if (window == null) {
                        setState {
                            copy(phase = EnrollmentPhase.Loaded, available = false, window = null, disciplines = emptyList())
                        }
                        return@launch
                    }
                    setState { copy(available = true, window = window) }
                    when (val offers = getOffers()) {
                        is Outcome.Ok -> setState {
                            val catalogue = offers.value.disciplines
                            copy(
                                phase = EnrollmentPhase.Loaded,
                                disciplines = catalogue,
                                offersFailed = false,
                                picks = if (initial) preseedPicks(window, catalogue) else picks,
                            )
                        }
                        is Outcome.Err -> {
                            if (initial) setState { copy(phase = EnrollmentPhase.Loaded, offersFailed = true) }
                        }
                    }
                }
            }
        }
    }

    // Tap semantics mirror the section-card footer: re-tapping the current
    // pick removes it; conflicting or full-without-queue sections are inert
    // (defense-in-depth — the card is visually disabled too).
    private fun sectionTapped(disciplineId: Long, sectionId: Long) {
        val state = currentState
        if (!state.canEdit) return
        val discipline = state.disciplineById(disciplineId) ?: return
        val section = discipline.sections.find { it.id == sectionId } ?: return
        val existing = state.pickFor(disciplineId)
        if (existing?.sectionId == sectionId) {
            analytics.selectContent(
                contentType = ContentTypes.OFFER,
                itemId = sectionId.toString(),
                properties = mapOf("action" to "remove"),
            )
            setState { copy(picks = picks.filter { it.disciplineId != disciplineId }) }
            return
        }
        if (state.clashFor(discipline, section) != null) return
        if (section.seats.isFull && state.window?.useQueue != true) return
        analytics.selectContent(
            contentType = ContentTypes.OFFER,
            itemId = sectionId.toString(),
            properties = mapOf("action" to "select"),
        )
        val pick = makePick(state.window, discipline, section)
        setState {
            copy(
                picks = if (existing != null) {
                    picks.map { if (it.disciplineId == disciplineId) pick else it }
                } else {
                    picks + pick
                },
            )
        }
    }

    // Submits the complete desired set; the backend replaces the saved
    // proposal wholesale (open → publish → close happens server-side). On
    // success the window flips to Closed locally so the status hub shows the
    // comprovante state without a refetch.
    private fun submit() {
        val state = currentState
        if (!state.canSubmit) return
        submitJob?.cancel()
        setState { copy(submitting = true, submitError = null) }
        submitJob = viewModelScope.launch {
            when (val outcome = submitEnrollment(state.selections)) {
                is Outcome.Ok -> {
                    analytics.selectContent(
                        contentType = ContentTypes.ENROLLMENT,
                        properties = mapOf("action" to "submit"),
                    )
                    setState {
                        copy(
                            submitting = false,
                            reopened = false,
                            window = window?.copy(state = EnrollmentWindowState.Closed),
                        )
                    }
                    emitEffect(EnrollmentEffect.Submitted)
                }
                is Outcome.Err -> setState { copy(submitting = false, submitError = outcome.error) }
            }
        }
    }
}
