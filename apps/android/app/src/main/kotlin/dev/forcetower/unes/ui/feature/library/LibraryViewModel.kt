package dev.forcetower.unes.ui.feature.library

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.library.domain.model.LibraryFacetGroup
import dev.forcetower.melon.feature.library.domain.model.LibraryFacetSelection
import dev.forcetower.melon.feature.library.domain.model.LibraryReading
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchPage
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchRequest
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchScope
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchTerm
import dev.forcetower.melon.feature.library.domain.model.LibrarySort
import dev.forcetower.melon.feature.library.domain.model.LibraryWork
import dev.forcetower.melon.feature.library.domain.usecase.CheckLibraryAvailabilityUseCase
import dev.forcetower.melon.feature.library.domain.usecase.ClearLibraryRecentsUseCase
import dev.forcetower.melon.feature.library.domain.usecase.GetLibraryOverviewUseCase
import dev.forcetower.melon.feature.library.domain.usecase.SearchLibraryUseCase
import dev.forcetower.unes.mvi.MviViewModel
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// One activity-scoped ViewModel for the whole Biblioteca stack (search entry,
// results, work detail). Nav3 entries all resolve the same instance — same
// trick as `ParadoxoViewModel` — so pushed screens keep their data on back
// navigation and availability readings are shared between the rows and the
// detail card. Mirrors iOS `LibraryFeature`/`LibraryResultsFeature`/
// `LibraryWorkDetailFeature` folded into one store: each pushed results
// screen owns a session (keyed by the id its route carries), like iOS's
// per-push feature state.
//
// Results ride androidx.paging: `pagingWorks(sessionId)` rebuilds whenever
// that session's query identity changes (terms, sort, facets) and appends
// pages of 25 as the list scrolls; the per-work availability consultation
// stays outside paging, triggered by row visibility.
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class LibraryViewModel @Inject constructor(
    private val getOverview: GetLibraryOverviewUseCase,
    private val searchLibrary: SearchLibraryUseCase,
    private val checkAvailability: CheckLibraryAvailabilityUseCase,
    private val clearRecents: ClearLibraryRecentsUseCase,
    private val analytics: Analytics,
) : MviViewModel<LibraryUiState, LibraryIntent, LibraryEffect>(LibraryUiState()) {

    private var overviewJob: Job? = null
    private var nextSessionId = 1
    private val checking = mutableSetOf<String>()
    private val pagers = mutableMapOf<Int, Flow<PagingData<LibraryWork>>>()

    // The slice of a session that identifies a pager — presentation toggles
    // (grouping, availability filter) deliberately excluded.
    private data class PagerIdentity(
        val terms: List<LibrarySearchTerm>,
        val sort: LibrarySort,
        val facets: LibraryFacetSelection,
        val tooBroad: Boolean,
    )

    // One cached paging flow per session: sort/facet changes swap the source
    // via flatMapLatest; back navigation re-collects the cached pages.
    fun pagingWorks(sessionId: Int): Flow<PagingData<LibraryWork>> =
        pagers.getOrPut(sessionId) {
            state
                .map { current ->
                    current.sessions[sessionId]?.let { session ->
                        PagerIdentity(
                            terms = session.terms,
                            sort = session.sort,
                            facets = session.facets,
                            tooBroad = session.isTooBroad,
                        )
                    }
                }
                .distinctUntilChanged()
                .flatMapLatest { identity ->
                    if (identity == null || identity.tooBroad) {
                        flowOf(PagingData.empty())
                    } else {
                        Pager(
                            config = PagingConfig(
                                pageSize = PAGE_SIZE,
                                prefetchDistance = PAGE_SIZE / 3,
                                enablePlaceholders = false,
                            ),
                        ) {
                            LibraryPagingSource(
                                search = searchLibrary,
                                request = LibrarySearchRequest(
                                    terms = identity.terms,
                                    sort = identity.sort,
                                    facets = identity.facets,
                                ),
                                onPage = { page -> onPageLoaded(sessionId, page) },
                            )
                        }.flow
                    }
                }
                .cachedIn(viewModelScope)
        }

    override fun onIntent(intent: LibraryIntent) {
        when (intent) {
            LibraryIntent.LoadOverview -> loadOverview(force = false)
            LibraryIntent.RetryOverview -> loadOverview(force = true)
            LibraryIntent.ClearRecents -> clearRecentsOptimistically()
            is LibraryIntent.SetSort -> updateSession(intent.sessionId) {
                copy(sort = intent.sort, total = null)
            }
            is LibraryIntent.ToggleFacet ->
                toggleFacet(intent.sessionId, intent.group, intent.key)
            is LibraryIntent.ClearFacets -> updateSession(intent.sessionId) {
                copy(facets = emptyMap(), total = null)
            }
            is LibraryIntent.SetOnlyAvailable -> updateSession(intent.sessionId) {
                copy(onlyAvailable = intent.enabled)
            }
            is LibraryIntent.SetGroupByType -> updateSession(intent.sessionId) {
                copy(groupByType = intent.enabled)
            }
            is LibraryIntent.RowShown -> ensureReading(intent.workId, force = false)
            is LibraryIntent.EnsureReading -> ensureReading(intent.workId, force = false)
            is LibraryIntent.RefreshReadings -> refreshReadings(intent.workIds)
        }
    }

    private fun loadOverview(force: Boolean) {
        if (!force && (currentState.overview != null || currentState.overviewLoading)) return
        if (overviewJob?.isActive == true) return
        setState { copy(overviewLoading = true, overviewFailed = false) }
        overviewJob = viewModelScope.launch {
            when (val outcome = getOverview()) {
                is Outcome.Ok -> setState {
                    copy(
                        overview = outcome.value,
                        overviewLoading = false,
                        overviewFailed = false,
                        recentsCleared = false,
                        // Seed the degraded-shape records so tapping a novidade
                        // pushes detail without another fetch.
                        works = works + outcome.value.newAcquisitions.associateBy { it.id },
                    )
                }
                // A stale overview beats an error screen; only fail empty.
                is Outcome.Err -> setState {
                    copy(overviewLoading = false, overviewFailed = overview == null)
                }
            }
        }
    }

    // Hide immediately, delete behind — failures are invisible by design
    // (the list simply comes back on the next overview fetch).
    private fun clearRecentsOptimistically() {
        setState { copy(recentsCleared = true) }
        viewModelScope.launch { clearRecents.invoke() }
    }

    // Creates the session a results push will render and returns the id its
    // route must carry. Not an intent because the caller needs the id back.
    fun startSearch(
        terms: List<LibrarySearchTerm>,
        initialFacets: LibraryFacetSelection = emptyMap(),
    ): Int {
        val cleaned = terms
            .map { it.copy(query = it.query.trim()) }
            .filter { it.query.isNotEmpty() }
            .take(MAX_TERMS)
        if (cleaned.isEmpty()) return 0
        val id = nextSessionId++
        setState {
            copy(
                sessions = sessions + (
                    id to LibrarySearchSession(id = id, terms = cleaned, facets = initialFacets)
                    ),
            )
        }
        return id
    }

    // Rebuilds a session lost to process death from the route's primary term.
    fun restoreSession(sessionId: Int, query: String, scope: LibrarySearchScope) {
        if (currentState.sessions.containsKey(sessionId)) return
        nextSessionId = max(nextSessionId, sessionId + 1)
        setState {
            copy(
                sessions = sessions + (
                    sessionId to LibrarySearchSession(
                        id = sessionId,
                        terms = listOf(LibrarySearchTerm(query, scope)),
                    )
                    ),
            )
        }
    }

    // "Buscar em todos os campos" — the empty-state broaden suggestion:
    // widens the primary term's scope in place, which rebuilds the pager.
    fun broadenScope(sessionId: Int) {
        updateSession(sessionId) {
            val first = terms.firstOrNull() ?: return@updateSession this
            copy(terms = listOf(first.copy(scope = LibrarySearchScope.All)), total = null)
        }
    }

    private fun toggleFacet(sessionId: Int, group: LibraryFacetGroup, key: String) {
        updateSession(sessionId) {
            val current = facets[group].orEmpty()
            val next = if (key in current) current - key else current + key
            val facets = if (next.isEmpty()) facets - group else facets + (group to next)
            copy(facets = facets, total = null)
        }
    }

    private fun updateSession(
        sessionId: Int,
        transform: LibrarySearchSession.() -> LibrarySearchSession,
    ) {
        setState {
            val session = sessions[sessionId] ?: return@setState this
            copy(sessions = sessions + (sessionId to session.transform()))
        }
    }

    // Called by the paging source as each page lands: the total and the facet
    // counts are identical on every page of the same query, so last-write-wins
    // is safe; every served work seeds the in-memory record map for detail.
    private fun onPageLoaded(sessionId: Int, page: LibrarySearchPage) {
        setState {
            val session = sessions[sessionId]
            copy(
                sessions = if (session == null) {
                    sessions
                } else {
                    sessions + (
                        sessionId to session.copy(
                            total = page.total,
                            serverFacets = page.facets.ifEmpty { session.serverFacets },
                        )
                        )
                },
                works = works + page.works.associateBy { it.id },
            )
        }
    }

    // One consultation per work: skip if a reading exists or one is in
    // flight. `checkAvailability` never throws — Pergamum going quiet comes
    // back as `Unavailable`, which the rows narrate.
    private fun ensureReading(workId: String, force: Boolean) {
        if (!force && currentState.readings.containsKey(workId)) return
        if (!checking.add(workId)) return
        viewModelScope.launch {
            val snapshot = checkAvailability(workId)
            checking.remove(workId)
            setState {
                copy(
                    readings = readings + (workId to snapshot.reading),
                    // Live copies supersede the scrape-time ones only when the
                    // circulation system actually answered.
                    liveCopies = if (
                        snapshot.reading != LibraryReading.Unavailable &&
                        snapshot.copies.isNotEmpty()
                    ) {
                        liveCopies + (workId to snapshot.copies)
                    } else {
                        liveCopies
                    },
                )
            }
        }
    }

    // "Atualizar" — re-consult only the rows already carrying a reading. The
    // old readings stay on screen while the pass runs; `refreshing` drives the
    // spinner in the freshness row.
    private fun refreshReadings(workIds: List<String>) {
        val toRefresh = workIds.filter { currentState.readings.containsKey(it) && checking.add(it) }
        if (toRefresh.isEmpty()) return
        setState { copy(refreshing = true) }
        viewModelScope.launch {
            toRefresh.forEach { workId ->
                val snapshot = checkAvailability(workId)
                checking.remove(workId)
                setState {
                    copy(
                        readings = readings + (workId to snapshot.reading),
                        liveCopies = if (
                            snapshot.reading != LibraryReading.Unavailable &&
                            snapshot.copies.isNotEmpty()
                        ) {
                            liveCopies + (workId to snapshot.copies)
                        } else {
                            liveCopies
                        },
                    )
                }
            }
            setState { copy(refreshing = false) }
        }
    }

    // Hands the full record to the detail push (the route carries only the
    // id) and reports the selection.
    fun openWork(work: LibraryWork) {
        setState { copy(works = works + (work.id to work)) }
        analytics.selectContent(contentType = ContentTypes.LIBRARY_WORK, itemId = work.id)
    }

    private companion object {
        const val PAGE_SIZE = 25
        const val MAX_TERMS = 3
    }
}
