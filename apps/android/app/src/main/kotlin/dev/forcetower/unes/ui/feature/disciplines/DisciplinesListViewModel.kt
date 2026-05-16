package dev.forcetower.unes.ui.feature.disciplines

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.disciplines.domain.usecase.ObserveDisciplinesListUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncSemesterUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlinx.coroutines.launch
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineListItem as KmpListItem
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplinesListState as KmpState
import dev.forcetower.melon.feature.disciplines.domain.model.ListGradeEntry as KmpGrade
import dev.forcetower.melon.feature.disciplines.domain.model.PendingSemester as KmpPending
import dev.forcetower.melon.feature.disciplines.domain.model.SemesterDisciplines as KmpSemester

// Drives `DisciplinesScreen`. Subscribes to the same `ObserveDisciplinesList`
// flow iOS uses (see `DisciplinesListViewModel.swift`); each emission maps the
// KMP payload into the local UI projection types in `DisciplinesModels.kt`.
// `download(semesterCode)` invokes `SyncSemesterUseCase` for a tapped pending
// placeholder card — DB writes trigger the flow to re-emit and reclassify the
// semester into `current` / `past`, so no local mutation is needed afterwards.
internal data class DisciplinesUiState(
    val current: Semester? = null,
    val past: List<Semester> = emptyList(),
    val pending: List<Semester> = emptyList(),
    val downloading: Set<String> = emptySet(),
    val downloadError: String? = null,
    // Seed handover for the detail route. Set when the list-card is tapped so
    // `DisciplineDetailRoute` can render the screen against the list payload
    // while its own VM hydrates from `ObserveDisciplineDetailUseCase`. Mirrors
    // the iOS pattern where `DisciplinesListView` constructs the detail VM
    // with the tapped Discipline as the seed.
    val openOfferId: String? = null,
    val openSeed: Discipline? = null,
) : UiState

internal sealed interface DisciplinesIntent : UiIntent {
    data class Download(val semesterCode: String) : DisciplinesIntent
    data class OpenDiscipline(val discipline: Discipline) : DisciplinesIntent
    data object CloseDiscipline : DisciplinesIntent
}

internal sealed interface DisciplinesEffect : UiEffect

@HiltViewModel
internal class DisciplinesListViewModel @Inject constructor(
    observeList: ObserveDisciplinesListUseCase,
    private val syncSemester: SyncSemesterUseCase,
) : MviViewModel<DisciplinesUiState, DisciplinesIntent, DisciplinesEffect>(DisciplinesUiState()) {

    init {
        viewModelScope.launch {
            observeList().collect { value -> apply(value) }
        }
    }

    override fun onIntent(intent: DisciplinesIntent) {
        when (intent) {
            is DisciplinesIntent.Download -> download(intent.semesterCode)
            is DisciplinesIntent.OpenDiscipline -> setState {
                copy(openOfferId = intent.discipline.offerId, openSeed = intent.discipline)
            }
            DisciplinesIntent.CloseDiscipline -> setState {
                copy(openOfferId = null, openSeed = null)
            }
        }
    }

    private fun apply(state: KmpState) {
        setState {
            copy(
                current = state.current?.let(::mapSemester),
                past = state.past.map(::mapSemester),
                pending = state.pending.map(::mapPending),
            )
        }
    }

    private fun download(semesterCode: String) {
        val pendingEntry = currentState.pending.firstOrNull { it.id == semesterCode }
        val dbId = pendingEntry?.dbSemesterId ?: return
        if (semesterCode in currentState.downloading) return

        viewModelScope.launch {
            setState { copy(downloading = downloading + semesterCode, downloadError = null) }
            val outcome = runCatching { syncSemester(dbId) }.getOrElse {
                setState { copy(downloading = downloading - semesterCode, downloadError = "Falha ao baixar o semestre.") }
                return@launch
            }
            val errorMessage = when (outcome) {
                is Outcome.Ok -> null
                is Outcome.Err -> "Falha ao baixar o semestre."
            }
            setState {
                copy(
                    downloading = downloading - semesterCode,
                    downloadError = errorMessage,
                )
            }
        }
    }
}

// ───────── KMP → UI projection ─────────

private fun mapSemester(raw: KmpSemester): Semester = Semester(
    id = raw.semesterCode,
    disciplines = raw.disciplines.map(::mapItem),
    isDownloaded = true,
    estimatedCount = null,
    dbSemesterId = raw.semesterId,
)

private fun mapPending(raw: KmpPending): Semester = Semester(
    id = raw.semesterCode,
    disciplines = emptyList(),
    isDownloaded = false,
    estimatedCount = null,
    dbSemesterId = raw.semesterId,
)

// Per-evaluation detail from upstream lands in a single "Geral" section here —
// section/group decomposition is a detail-view concern. Color is resolved at
// the call site from the composable palette in `DisciplinesScreen` (we can't
// build a Color here without a Composable context).
private fun mapItem(raw: KmpListItem): Discipline {
    val section = GradeSection(
        name = "Geral",
        group = null,
        grades = raw.grades.map(::mapGrade),
    )
    val groups = buildGroups(raw.groupsLabel)
    return Discipline(
        code = raw.code,
        fullCode = raw.code,
        title = raw.name,
        dept = raw.department.orEmpty(),
        prof = raw.teacherName.orEmpty(),
        // Placeholder — overwritten by the screen with the palette-resolved
        // color (palettes need a Composable context).
        color = androidx.compose.ui.graphics.Color.Unspecified,
        hours = raw.hours,
        absences = raw.missedHours,
        allowedAbsences = raw.allowedMissedHours,
        sections = listOf(section),
        groups = groups,
        finalGrade = raw.finalGrade,
        approved = raw.approved,
        storedPartialAverage = raw.partialAverage,
        disciplineId = raw.disciplineId,
        offerId = raw.offerId,
        semesterId = raw.semesterId,
    )
}

private fun mapGrade(raw: KmpGrade): GradeEntry = GradeEntry(
    label = raw.nameShort ?: raw.name,
    title = raw.name,
    date = DisciplineDateFormatting.ddMmYyyy(raw.date),
    score = raw.value,
)

// "Te · Pr"-style label from the upstream slug. Empty/unknown labels become an
// empty group list — `Discipline.hasMultipleGroups` then returns false.
private fun buildGroups(label: String?): List<DisciplineGroup> {
    if (label.isNullOrEmpty()) return emptyList()
    return label.split(" · ").filter { it.isNotBlank() }.map { kind ->
        DisciplineGroup(code = "", kind = kind, prof = "")
    }
}
