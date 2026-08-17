package dev.forcetower.unes.ui.feature.courseprogress

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgress
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgressError
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumPeriod
import dev.forcetower.melon.feature.courseprogress.domain.usecase.ObserveCourseProgressUseCase
import dev.forcetower.melon.feature.courseprogress.domain.usecase.RefreshCourseProgressUseCase
import dev.forcetower.melon.feature.courseprogress.domain.usecase.ResetCurriculumVersionUseCase
import dev.forcetower.melon.feature.courseprogress.domain.usecase.SelectCurriculumVersionUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

// The three lenses the fluxograma offers over the same grid.
internal enum class CurriculumLens { Periods, Map, Grid }

// The prerequisite chain being highlighted: everything outside `codes` dims,
// `focus` gets the ring.
internal data class CurriculumTrail(val focus: String, val codes: Set<String>)

internal data class CourseProgressUiState(
    val progress: CourseProgress? = null,
    // A refresh is in flight and nothing is mirrored yet.
    val loading: Boolean = true,
    // The refresh failed and nothing is mirrored to fall back on.
    val failed: Boolean = false,
    // The course name comes from the profile — the payload carries the
    // curriculum, not the course.
    val course: String? = null,
    val lens: CurriculumLens = CurriculumLens.Periods,
    // Null until a payload arrives; then it settles on the student's período.
    val selectedPeriod: Int? = null,
    val trail: CurriculumTrail? = null,
    // The discipline whose sheet is open, by code.
    val openedEntryCode: String? = null,
    val explainerOpen: Boolean = false,
    val versionPickerOpen: Boolean = false,
    // The version whose PUT (or the DELETE back to automatic, as
    // [AUTOMATIC_VERSION_SWITCH]) is in flight — the picker locks and spins
    // on it meanwhile.
    val switchingVersionId: String? = null,
    // The last switch failed; the dialog stays up until dismissed.
    val versionSwitchFailed: Boolean = false,
) : UiState {
    val selectedPeriodEntries: CurriculumPeriod?
        get() = selectedPeriod?.let { progress?.period(it) }

    val openedEntry get() = openedEntryCode?.let { progress?.entry(it) }

    val isSwitchingVersion: Boolean get() = switchingVersionId != null
}

// Sentinel for `switchingVersionId` while resetting to the server's resolution
// rather than picking a version.
internal const val AUTOMATIC_VERSION_SWITCH = "automatic"

internal sealed interface CourseProgressIntent : UiIntent {
    data object Load : CourseProgressIntent
    data object Retry : CourseProgressIntent
    data class LensChanged(val lens: CurriculumLens) : CourseProgressIntent
    data class PeriodSelected(val period: Int) : CourseProgressIntent
    data class EntryTapped(val code: String) : CourseProgressIntent
    data object EntrySheetDismissed : CourseProgressIntent
    data class TrailRequested(val code: String) : CourseProgressIntent
    data object TrailCleared : CourseProgressIntent
    data object ExplainerTapped : CourseProgressIntent
    data object ExplainerDismissed : CourseProgressIntent
    data object VersionPickerTapped : CourseProgressIntent
    data object VersionPickerDismissed : CourseProgressIntent
    data class VersionSelected(val curriculumId: String) : CourseProgressIntent
    data object AutomaticVersionTapped : CourseProgressIntent
    data object VersionSwitchFailureDismissed : CourseProgressIntent
}

internal sealed interface CourseProgressEffect : UiEffect

// Shared by "Progresso do curso" and the fluxograma — both routes resolve the
// same activity-scoped instance, so pushing the grid costs no second fetch and
// the lens/selection survive a pop back to the progress screen.
//
// Offline-first, mirroring iOS `CourseProgressFeature`: the observation replays
// the Room mirror on subscription so the screen paints without the network,
// and every entry fires a refresh whose result lands through that same stream.
@HiltViewModel
internal class CourseProgressViewModel @Inject constructor(
    observeCourseProgress: ObserveCourseProgressUseCase,
    observeMeProfile: ObserveMeProfileUseCase,
    private val refreshCourseProgress: RefreshCourseProgressUseCase,
    private val selectCurriculumVersion: SelectCurriculumVersionUseCase,
    private val resetCurriculumVersion: ResetCurriculumVersionUseCase,
    private val analytics: Analytics,
) : MviViewModel<CourseProgressUiState, CourseProgressIntent, CourseProgressEffect>(
    CourseProgressUiState(),
) {
    // Re-entering the feature while the first refresh is still in flight must
    // not stack a second one.
    private val refreshMutex = Mutex()

    init {
        viewModelScope.launch {
            observeCourseProgress().collect { value -> onProgress(value) }
        }
        viewModelScope.launch {
            observeMeProfile()
                .map { it.identity.courseName }
                .collect { value -> setState { copy(course = value) } }
        }
    }

    override fun onIntent(intent: CourseProgressIntent) {
        when (intent) {
            CourseProgressIntent.Load -> refresh()
            CourseProgressIntent.Retry -> {
                setState { copy(loading = true, failed = false) }
                refresh()
            }
            is CourseProgressIntent.LensChanged -> selectLens(intent.lens)
            is CourseProgressIntent.PeriodSelected -> selectPeriod(intent.period)
            is CourseProgressIntent.EntryTapped -> openEntry(intent.code)
            CourseProgressIntent.EntrySheetDismissed -> setState { copy(openedEntryCode = null) }
            is CourseProgressIntent.TrailRequested -> showTrail(intent.code)
            CourseProgressIntent.TrailCleared -> setState { copy(trail = null) }
            CourseProgressIntent.ExplainerTapped -> {
                analytics.selectContent(ContentTypes.HUB, "complementary_hours_explainer")
                setState { copy(explainerOpen = true) }
            }
            CourseProgressIntent.ExplainerDismissed -> setState { copy(explainerOpen = false) }
            CourseProgressIntent.VersionPickerTapped -> openVersionPicker()
            CourseProgressIntent.VersionPickerDismissed -> setState { copy(versionPickerOpen = false) }
            is CourseProgressIntent.VersionSelected -> selectVersion(intent.curriculumId)
            CourseProgressIntent.AutomaticVersionTapped -> resetVersion()
            CourseProgressIntent.VersionSwitchFailureDismissed -> setState { copy(versionSwitchFailed = false) }
        }
    }

    // Fired when the grade card is tapped — the fluxograma is only reachable
    // with a grid to lay out.
    fun trackFlowchartOpen() {
        analytics.selectContent(ContentTypes.HUB, "curriculum_flow")
    }

    private fun onProgress(value: CourseProgress?) = setState {
        if (value == null) return@setState this
        copy(
            progress = value,
            loading = false,
            failed = false,
            // Keep the student's own choice as long as it still exists in the
            // refreshed grid; otherwise land on their período.
            selectedPeriod = selectedPeriod?.takeIf { value.period(it) != null }
                ?: value.landingPeriod,
            trail = trail?.takeIf { value.entry(it.focus) != null },
            openedEntryCode = openedEntryCode?.takeIf { value.entry(it) != null },
        )
    }

    private fun refresh() = viewModelScope.launch {
        if (!refreshMutex.tryLock()) return@launch
        try {
            val outcome = refreshCourseProgress()
            setState {
                // A stale screen beats an error screen.
                copy(loading = false, failed = outcome is Outcome.Err && progress == null)
            }
        } finally {
            refreshMutex.unlock()
        }
    }

    private fun openVersionPicker() {
        if (currentState.progress?.canPickVersion != true) return
        analytics.selectContent(ContentTypes.HUB, "curriculum_version_picker")
        setState { copy(versionPickerOpen = true) }
    }

    private fun selectVersion(curriculumId: String) {
        val state = currentState
        if (state.isSwitchingVersion) return
        // Re-picking the bound version is a no-op: nothing to send, just close.
        if (state.progress?.curriculum?.id == curriculumId) {
            setState { copy(versionPickerOpen = false) }
            return
        }
        analytics.selectContent(ContentTypes.CURRICULUM_VERSION, curriculumId)
        switchVersion(curriculumId) { selectCurriculumVersion(curriculumId) }
    }

    private fun resetVersion() {
        val state = currentState
        if (state.isSwitchingVersion || state.progress?.curriculum?.isManualPick != true) return
        analytics.selectContent(ContentTypes.CURRICULUM_VERSION, AUTOMATIC_VERSION_SWITCH)
        switchVersion(AUTOMATIC_VERSION_SWITCH) { resetCurriculumVersion() }
    }

    // The rebuilt payload lands through the observation; the sheet closes over
    // the new numbers, or stays up behind the failure dialog.
    private fun switchVersion(
        switchingId: String,
        request: suspend () -> Outcome<Unit, CourseProgressError>,
    ) {
        setState { copy(switchingVersionId = switchingId) }
        viewModelScope.launch {
            val outcome = request()
            setState {
                copy(
                    switchingVersionId = null,
                    versionPickerOpen = outcome !is Outcome.Ok,
                    versionSwitchFailed = outcome is Outcome.Err,
                )
            }
        }
    }

    private fun selectLens(lens: CurriculumLens) {
        if (currentState.lens == lens) return
        analytics.selectContent(
            ContentTypes.TILE,
            "curriculum_lens",
            mapOf("lens" to lens.name.lowercase()),
        )
        setState { copy(lens = lens) }
    }

    private fun selectPeriod(period: Int) = setState {
        if (progress?.period(period) == null) return@setState this
        // Jumping to a período from the map is a request to read its names.
        copy(selectedPeriod = period, lens = if (lens == CurriculumLens.Map) CurriculumLens.Periods else lens)
    }

    private fun openEntry(code: String) {
        if (currentState.progress?.entry(code) == null) return
        analytics.selectContent(
            ContentTypes.DISCIPLINE,
            code,
            mapOf("source" to "curriculum_flow"),
        )
        setState { copy(openedEntryCode = code) }
    }

    private fun showTrail(code: String) {
        val progress = currentState.progress ?: return
        if (progress.entry(code) == null) return
        analytics.selectContent(ContentTypes.TILE, "curriculum_trail", mapOf("code" to code))
        // Co-requisites ride along: they are taken with the focus, so dimming
        // them would misread the chain.
        val codes = progress.trail(code) + progress.corequisites(code).map { it.code }
        setState {
            copy(
                trail = CurriculumTrail(focus = code, codes = codes),
                openedEntryCode = null,
                lens = CurriculumLens.Map,
            )
        }
    }
}
