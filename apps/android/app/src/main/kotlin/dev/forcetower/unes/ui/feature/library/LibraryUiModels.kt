package dev.forcetower.unes.ui.feature.library

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import dev.forcetower.melon.feature.library.domain.model.LibraryBranch
import dev.forcetower.melon.feature.library.domain.model.LibraryCopy
import dev.forcetower.melon.feature.library.domain.model.LibraryCopyStatus
import dev.forcetower.melon.feature.library.domain.model.LibraryFacetGroup
import dev.forcetower.melon.feature.library.domain.model.LibraryFacetSelection
import dev.forcetower.melon.feature.library.domain.model.LibraryFacetValue
import dev.forcetower.melon.feature.library.domain.model.LibraryOverview
import dev.forcetower.melon.feature.library.domain.model.LibraryReading
import dev.forcetower.melon.feature.library.domain.model.LibraryRecordField
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchScope
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchTerm
import dev.forcetower.melon.feature.library.domain.model.LibrarySort
import dev.forcetower.melon.feature.library.domain.model.LibraryWork
import dev.forcetower.melon.feature.library.domain.model.LibraryWorkTitle
import dev.forcetower.melon.feature.library.domain.model.LibraryWorkType
import dev.forcetower.melon.feature.library.domain.model.LibraryYearBucket
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState

// State/intents for the whole Biblioteca stack (entry, results, work detail),
// held by the shared activity-scoped `LibraryViewModel`, plus the small
// presentation mappings (type glyphs/hues, scope/sort/facet labels) the
// screens share.

internal data class LibraryUiState(
    // Search entry (`api/library/overview`).
    val overview: LibraryOverview? = null,
    val overviewLoading: Boolean = false,
    val overviewFailed: Boolean = false,
    val recentsCleared: Boolean = false,
    // Results sessions keyed by the id carried in the `LibraryResults` route —
    // each pushed results screen keeps its own query state, like iOS's
    // per-push `LibraryResultsFeature`.
    val sessions: Map<Int, LibrarySearchSession> = emptyMap(),
    // Availability readings and the live copies that superseded the scrape-time
    // ones, both keyed by work id and shared by rows and the detail screen.
    val readings: Map<String, LibraryReading> = emptyMap(),
    val liveCopies: Map<String, List<LibraryCopy>> = emptyMap(),
    // An "Atualizar" pass is re-consulting the rows on screen.
    val refreshing: Boolean = false,
    // Works handed off in-memory to the detail push (route carries only the id).
    val works: Map<String, LibraryWork> = emptyMap(),
) : UiState

// One results screen worth of query state. Terms/sort/facets are the pager
// identity (changing them rebuilds the source); `onlyAvailable` and
// `groupByType` are presentation-only and leave the loaded pages alone.
internal data class LibrarySearchSession(
    val id: Int,
    val terms: List<LibrarySearchTerm>,
    val sort: LibrarySort = LibrarySort.Relevance,
    val facets: LibraryFacetSelection = emptyMap(),
    val serverFacets: Map<LibraryFacetGroup, List<LibraryFacetValue>> = emptyMap(),
    val total: Int? = null,
    val onlyAvailable: Boolean = false,
    val groupByType: Boolean = false,
) {
    // Pergamum gives up after 30s on unrestricted single-letter sweeps, so the
    // client refuses to even ask — same guard as iOS `isTooBroad`.
    val isTooBroad: Boolean
        get() = terms.size == 1 && terms.first().query.trim().length <= 2 && facets.isEmpty()

    val activeFacetCount: Int get() = facets.values.sumOf { it.size }
}

internal sealed interface LibraryIntent : UiIntent {
    data object LoadOverview : LibraryIntent
    data object RetryOverview : LibraryIntent
    data object ClearRecents : LibraryIntent
    data class SetSort(val sessionId: Int, val sort: LibrarySort) : LibraryIntent
    data class ToggleFacet(
        val sessionId: Int,
        val group: LibraryFacetGroup,
        val key: String,
    ) : LibraryIntent
    data class ClearFacets(val sessionId: Int) : LibraryIntent
    data class SetOnlyAvailable(val sessionId: Int, val enabled: Boolean) : LibraryIntent
    data class SetGroupByType(val sessionId: Int, val enabled: Boolean) : LibraryIntent
    // A results row composed on screen — kicks the lazy availability check.
    data class RowShown(val workId: String) : LibraryIntent
    // Detail `.task` — check unless a reading already exists.
    data class EnsureReading(val workId: String) : LibraryIntent
    // "Atualizar" — re-consults the rows already carrying a reading.
    data class RefreshReadings(val workIds: List<String>) : LibraryIntent
}

internal sealed interface LibraryEffect : UiEffect

// ───────── presentation mappings ─────────

@Composable
internal fun LibraryWorkType.icon(): ImageVector = when (this) {
    LibraryWorkType.Book -> Icons.AutoMirrored.Filled.MenuBook
    LibraryWorkType.Pamphlet -> Icons.Filled.Description
    LibraryWorkType.Cordel -> Icons.Filled.AutoStories
    LibraryWorkType.EducationalProduct -> Icons.Filled.Inventory2
    LibraryWorkType.Dissertation, LibraryWorkType.Thesis -> Icons.Filled.School
    LibraryWorkType.Article -> Icons.AutoMirrored.Filled.Article
    LibraryWorkType.Periodical -> Icons.Filled.Newspaper
}

// Books are the overwhelming majority, so they read neutral; the regional and
// academic tail gets the palette accents (mirrors the dc type hue map).
@Composable
internal fun LibraryWorkType.hue(): Color = when (this) {
    LibraryWorkType.Book -> MaterialTheme.colorScheme.onSurfaceVariant
    LibraryWorkType.Pamphlet -> MaterialTheme.melon.palette.amber
    LibraryWorkType.Cordel -> MaterialTheme.melon.palette.coral
    LibraryWorkType.EducationalProduct -> MaterialTheme.melon.palette.violet
    LibraryWorkType.Dissertation, LibraryWorkType.Thesis -> MaterialTheme.melon.palette.magenta
    LibraryWorkType.Article -> MaterialTheme.melon.palette.teal
    LibraryWorkType.Periodical -> MaterialTheme.melon.palette.indigo
}

@StringRes
internal fun LibraryWorkType.labelRes(): Int = when (this) {
    LibraryWorkType.Book -> R.string.library_type_book
    LibraryWorkType.Pamphlet -> R.string.library_type_pamphlet
    LibraryWorkType.Cordel -> R.string.library_type_cordel
    LibraryWorkType.EducationalProduct -> R.string.library_type_product
    LibraryWorkType.Dissertation -> R.string.library_type_dissertation
    LibraryWorkType.Thesis -> R.string.library_type_thesis
    LibraryWorkType.Article -> R.string.library_type_article
    LibraryWorkType.Periodical -> R.string.library_type_periodical
}

@StringRes
internal fun LibraryWorkType.pluralLabelRes(): Int = when (this) {
    LibraryWorkType.Book -> R.string.library_type_book_plural
    LibraryWorkType.Pamphlet -> R.string.library_type_pamphlet_plural
    LibraryWorkType.Cordel -> R.string.library_type_cordel_plural
    LibraryWorkType.EducationalProduct -> R.string.library_type_product_plural
    LibraryWorkType.Dissertation -> R.string.library_type_dissertation_plural
    LibraryWorkType.Thesis -> R.string.library_type_thesis_plural
    LibraryWorkType.Article -> R.string.library_type_article_plural
    LibraryWorkType.Periodical -> R.string.library_type_periodical_plural
}

@StringRes
internal fun LibrarySearchScope.labelRes(): Int = when (this) {
    LibrarySearchScope.All -> R.string.library_scope_all
    LibrarySearchScope.Title -> R.string.library_scope_title
    LibrarySearchScope.Author -> R.string.library_scope_author
    LibrarySearchScope.Subject -> R.string.library_scope_subject
    LibrarySearchScope.Isbn -> R.string.library_scope_isbn
    LibrarySearchScope.CallNumber -> R.string.library_scope_call
}

@StringRes
internal fun LibrarySort.labelRes(): Int = when (this) {
    LibrarySort.Relevance -> R.string.library_sort_relevance
    LibrarySort.Newest -> R.string.library_sort_newest
    LibrarySort.Oldest -> R.string.library_sort_oldest
    LibrarySort.TitleAZ -> R.string.library_sort_title
}

@StringRes
internal fun LibraryFacetGroup.labelRes(): Int = when (this) {
    LibraryFacetGroup.Type -> R.string.library_facet_type
    LibraryFacetGroup.Branch -> R.string.library_facet_branch
    LibraryFacetGroup.Subject -> R.string.library_facet_subject
    LibraryFacetGroup.Author -> R.string.library_facet_author
    LibraryFacetGroup.Language -> R.string.library_facet_language
    LibraryFacetGroup.Year -> R.string.library_facet_year
}

// Facet value labels: type and year resolve to localized bucket names,
// branches resolve through the known-branch registry, and free-text groups
// (subject/author/language) trust the server label — same rules as iOS.
@Composable
internal fun facetValueLabel(group: LibraryFacetGroup, value: LibraryFacetValue): String =
    when (group) {
        LibraryFacetGroup.Type ->
            LibraryWorkType.fromWire(value.key)
                ?.let { stringResource(it.pluralLabelRes()) }
                ?: value.label
        LibraryFacetGroup.Year ->
            LibraryYearBucket.fromWire(value.key)
                ?.let { stringResource(it.labelRes()) }
                ?: value.label
        LibraryFacetGroup.Branch ->
            LibraryBranch.known.firstOrNull { it.id == value.key }?.shortName ?: value.label
        LibraryFacetGroup.Subject, LibraryFacetGroup.Author, LibraryFacetGroup.Language ->
            value.label
    }

@StringRes
internal fun LibraryYearBucket.labelRes(): Int = when (this) {
    LibraryYearBucket.From2020 -> R.string.library_year_from2020
    LibraryYearBucket.Decade2010 -> R.string.library_year_2010
    LibraryYearBucket.Decade2000 -> R.string.library_year_2000
    LibraryYearBucket.Decade1990 -> R.string.library_year_1990
    LibraryYearBucket.Before1990 -> R.string.library_year_old
    LibraryYearBucket.Unknown -> R.string.library_year_none
}

// Preview fixture — the design's flagship record (Anton's Cálculo), trimmed.
internal fun libraryPreviewWork(): LibraryWork = LibraryWork(
    id = "4278",
    rawTitle = "Cálculo : um novo horizonte - 6. ed / 0000",
    callNumber = "515 A638c",
    type = LibraryWorkType.Book,
    rawYear = "2000",
    authors = listOf("Anton, Howard", "Bivens, Irl", "Davis, Stephen"),
    subjects = listOf("Cálculo", "Matemática", "Geometria analítica"),
    branches = listOf(LibraryBranch.central),
    rawIsbn = "85-7307-655-3: (Broch.)",
    language = "Portuguese",
    collection = null,
    series = null,
    reference = "ANTON, Howard; BIVENS, Irl; DAVIS, Stephen. **Cálculo:** um novo horizonte. " +
        "6. ed. Porto Alegre: Bookman, 2000. 2 v.",
    record = listOf(
        LibraryRecordField("Número de Chamada", "515 A638c"),
        LibraryRecordField("Autor Principal", "Anton, Howard"),
    ),
    copies = List(3) {
        LibraryCopy(
            branch = LibraryBranch.central,
            area = "Coleção Geral",
            callNumber = "515 A638c 6. ed v. 1",
            status = LibraryCopyStatus.Available,
        )
    },
    serverTitle = LibraryWorkTitle(
        title = "Cálculo",
        subtitle = "um novo horizonte",
        edition = "6. ed",
        junkYear = "0000",
    ),
)
