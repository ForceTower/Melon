package dev.forcetower.unes.ui.feature.finalcountdown

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.feature.disciplines.domain.usecase.ObserveDisciplinesListUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import dev.forcetower.unes.ui.feature.disciplines.formatSemesterCode
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineListItem as KmpListItem
import dev.forcetower.melon.feature.disciplines.domain.model.ListGradeEntry as KmpGrade

// Drives the Final Countdown screen. Mirrors iOS `FinalCountdownFeature`: the
// picker choices come from the current semester of the same disciplines flow
// the Disciplinas tab uses; picking one seeds the editable rows from its
// released grades (Prova Final rows excluded — they never average in). A null
// `discipline` is "modo livre": blank rows, pure hypotheticals.
internal data class FinalCountdownUiState(
    val rows: List<FCRow> = freeRows(),
    val weighted: Boolean = false,
    val discipline: FCDiscipline? = null,
    val choices: List<FCDiscipline> = emptyList(),
    // "2026.2" — feeds the failed verdict's "Volta em …" lead.
    val nextSemesterLabel: String? = null,
) : UiState {
    companion object {
        // Blank trio for modo livre — labels match the dc default rows.
        fun freeRows(): List<FCRow> = listOf(
            FCRow(label = "VA1"),
            FCRow(label = "VA2"),
            FCRow(label = "Trab"),
        )
    }
}

internal sealed interface FinalCountdownIntent : UiIntent {
    data class RowLabelChanged(val id: String, val label: String) : FinalCountdownIntent
    data class RowScoreChanged(val id: String, val text: String) : FinalCountdownIntent
    data class RowWeightChanged(val id: String, val delta: Int) : FinalCountdownIntent
    data class RemoveRow(val id: String) : FinalCountdownIntent
    data object AddRow : FinalCountdownIntent
    data object ToggleWeighted : FinalCountdownIntent
    data object Reset : FinalCountdownIntent
    // Null is "modo livre" — hypotheticals with no discipline attached.
    data class PickDiscipline(val offerId: String?) : FinalCountdownIntent
    // Route payload — pre-selects a discipline when the screen is pushed from
    // a context that already has one (e.g. a discipline detail CTA).
    data class SeedFromRoute(val offerId: String?) : FinalCountdownIntent
}

internal sealed interface FinalCountdownEffect : UiEffect

@HiltViewModel
internal class FinalCountdownViewModel @Inject constructor(
    observeList: ObserveDisciplinesListUseCase,
    private val analytics: Analytics,
) : MviViewModel<FinalCountdownUiState, FinalCountdownIntent, FinalCountdownEffect>(
    FinalCountdownUiState(),
) {
    // Route seed waiting for the choices flow to emit the matching discipline.
    private var pendingRouteSeed: String? = null

    init {
        viewModelScope.launch {
            observeList().collect { list ->
                // Between semesters (`current` is date-scoped to today) fall
                // back to the most recent downloaded semester — simulating the
                // one that just closed is still the calculator's main job.
                val semester = list.current ?: list.past.firstOrNull()
                val semesterLabel = semester?.semesterCode?.let(::formatSemesterCode).orEmpty()
                val choices = semester?.disciplines.orEmpty().map { mapChoice(it, semesterLabel) }
                setState {
                    copy(
                        choices = choices,
                        nextSemesterLabel = semester?.semesterCode?.let(::nextSemesterLabel),
                        // Keep the attached discipline's header fresh without
                        // reseeding the rows the student may have edited.
                        discipline = discipline?.let { open ->
                            choices.firstOrNull { it.offerId == open.offerId } ?: open
                        },
                    )
                }
                pendingRouteSeed?.let { seed ->
                    if (choices.any { it.offerId == seed }) {
                        pendingRouteSeed = null
                        pick(seed)
                    }
                }
            }
        }
    }

    override fun onIntent(intent: FinalCountdownIntent) {
        when (intent) {
            is FinalCountdownIntent.RowLabelChanged -> updateRow(intent.id) {
                copy(label = intent.label.take(6))
            }
            is FinalCountdownIntent.RowScoreChanged -> updateRow(intent.id) {
                copy(scoreText = FCRow.sanitizeScoreText(intent.text))
            }
            is FinalCountdownIntent.RowWeightChanged -> updateRow(intent.id) {
                copy(weight = (weight + intent.delta).coerceIn(1, 9))
            }
            is FinalCountdownIntent.RemoveRow -> setState {
                if (rows.size <= 1) this else copy(rows = rows.filterNot { it.id == intent.id })
            }
            FinalCountdownIntent.AddRow -> setState {
                copy(rows = rows + FCRow(label = "VA${rows.size + 1}"))
            }
            FinalCountdownIntent.ToggleWeighted -> setState { copy(weighted = !weighted) }
            FinalCountdownIntent.Reset -> setState {
                copy(
                    rows = discipline?.let { blankSeededRows(it) }
                        ?: FinalCountdownUiState.freeRows(),
                )
            }
            is FinalCountdownIntent.PickDiscipline -> {
                intent.offerId?.let {
                    analytics.selectContent(ContentTypes.DISCIPLINE, it, mapOf("action" to "simulate"))
                }
                pick(intent.offerId)
            }
            is FinalCountdownIntent.SeedFromRoute -> {
                val offerId = intent.offerId ?: return
                if (currentState.choices.any { it.offerId == offerId }) {
                    pick(offerId)
                } else {
                    pendingRouteSeed = offerId
                }
            }
        }
    }

    private fun pick(offerId: String?) {
        if (offerId == null) {
            setState { copy(discipline = null, rows = FinalCountdownUiState.freeRows()) }
            return
        }
        val choice = currentState.choices.firstOrNull { it.offerId == offerId } ?: return
        setState { copy(discipline = choice, rows = seededRows(choice)) }
    }

    private fun updateRow(id: String, transform: FCRow.() -> FCRow) {
        setState { copy(rows = rows.map { if (it.id == id) it.transform() else it }) }
    }
}

// ───────── KMP → UI projection + seeding ─────────

private fun mapChoice(raw: KmpListItem, semesterLabel: String): FCDiscipline = FCDiscipline(
    offerId = raw.offerId,
    code = raw.code,
    name = raw.name,
    teacher = raw.teacherName?.takeIf { it.isNotBlank() },
    semesterLabel = semesterLabel,
    seedGrades = raw.grades.filterNot(::isProvaFinal).map { grade ->
        FCSeedGrade(
            label = (grade.nameShort ?: grade.name).take(6),
            value = grade.value,
            weight = grade.weight,
        )
    },
)

// The Prova Final slot (upstream "Notas Complementares", name "Prova Final",
// short name "Adicional") must never seed the calculator — it would count as
// a pending evaluation and drag every projection down. Same exclusion iOS
// applies at the model layer (`DisciplineDetail.finalExam`).
private fun isProvaFinal(grade: KmpGrade): Boolean =
    grade.name.trim().equals("Prova Final", ignoreCase = true) ||
        grade.nameShort?.trim().equals("Adicional", ignoreCase = true)

private fun seededRows(choice: FCDiscipline): List<FCRow> {
    if (choice.seedGrades.isEmpty()) return FinalCountdownUiState.freeRows()
    return choice.seedGrades.map { grade ->
        FCRow(
            label = grade.label,
            scoreText = FCRow.text(grade.value),
            weight = grade.weight?.roundToInt()?.coerceIn(1, 9) ?: 1,
        )
    }
}

// "Limpar tudo" with a discipline attached keeps its evaluation structure
// (labels + weights) and clears only the typed scores.
private fun blankSeededRows(choice: FCDiscipline): List<FCRow> =
    seededRows(choice).map { it.copy(scoreText = "") }

// "20261" → "2026.2", "20262" → "2027.1". Mirrors iOS
// `FCDiscipline.nextSemesterLabel`.
private fun nextSemesterLabel(code: String): String? {
    if (code.length != 5 || !code.all { it.isDigit() }) return null
    val year = code.take(4).toInt()
    return when (code.last()) {
        '1' -> "$year.2"
        '2' -> "${year + 1}.1"
        else -> null
    }
}
