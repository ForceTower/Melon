package dev.forcetower.unes.ui.feature.paradoxo

import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoDisciplineDetail
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoIndexEntry
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoOverview
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoRef
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoTeacherDetail
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import java.text.Normalizer
import java.util.Locale

// Index entry + its pre-folded search key so typing doesn't re-normalize the
// whole catalogue on every keystroke.
internal data class ParadoxoIndexItem(
    val entry: ParadoxoIndexEntry,
    val searchKey: String,
)

// Per-id fetch state for the detail pushes. Kept in a map so a
// discipline → teacher → discipline chain preserves each screen's data on
// back navigation (the ViewModel is activity-scoped and shared).
internal sealed interface ParadoxoDetail<out T> {
    data object Loading : ParadoxoDetail<Nothing>
    data object Failed : ParadoxoDetail<Nothing>
    data class Loaded<T>(val data: T) : ParadoxoDetail<T>
}

internal data class ParadoxoUiState(
    val loading: Boolean = false,
    val failed: Boolean = false,
    val overview: ParadoxoOverview? = null,
    val index: List<ParadoxoIndexItem> = emptyList(),
    val query: String = "",
    val avatarInitial: String? = null,
    val disciplines: Map<String, ParadoxoDetail<ParadoxoDisciplineDetail>> = emptyMap(),
    val teachers: Map<String, ParadoxoDetail<ParadoxoTeacherDetail>> = emptyMap(),
) : UiState

internal sealed interface ParadoxoIntent : UiIntent {
    data object Load : ParadoxoIntent
    data object Retry : ParadoxoIntent
    data class QueryChanged(val query: String) : ParadoxoIntent
    data class LoadDiscipline(val id: String) : ParadoxoIntent
    data class RetryDiscipline(val id: String) : ParadoxoIntent
    data class LoadTeacher(val id: String) : ParadoxoIntent
    data class RetryTeacher(val id: String) : ParadoxoIntent
}

internal sealed interface ParadoxoEffect : UiEffect

internal data class ParadoxoSearchResults(
    val disciplines: List<ParadoxoIndexEntry>,
    val teachers: List<ParadoxoIndexEntry>,
) {
    val isEmpty: Boolean get() = disciplines.isEmpty() && teachers.isEmpty()
}

private val MarksRegex = Regex("\\p{Mn}+")
private val PtBr = Locale("pt", "BR")

// Diacritic/case fold, mirroring iOS's `folding(options:locale:)` — "Cálculo"
// and "calculo" match each other.
internal fun paradoxoFold(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(MarksRegex, "")
        .lowercase(PtBr)

// Every whitespace-separated term must appear somewhere in the entry's folded
// "code name" key. Caps mirror iOS (10 disciplines, 12 teachers).
internal fun searchParadoxo(
    index: List<ParadoxoIndexItem>,
    query: String,
): ParadoxoSearchResults {
    val terms = paradoxoFold(query.trim()).split(' ').filter { it.isNotBlank() }
    val matches = index.filter { item -> terms.all { term -> item.searchKey.contains(term) } }
    return ParadoxoSearchResults(
        disciplines = matches.filter { it.entry.ref is ParadoxoRef.Discipline }
            .take(10)
            .map { it.entry },
        teachers = matches.filter { it.entry.ref is ParadoxoRef.Teacher }
            .take(12)
            .map { it.entry },
    )
}
