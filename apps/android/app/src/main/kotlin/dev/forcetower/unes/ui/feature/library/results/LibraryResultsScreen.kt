package dev.forcetower.unes.ui.feature.library.results

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import dev.forcetower.melon.feature.library.domain.model.LibraryAvailability
import dev.forcetower.melon.feature.library.domain.model.LibraryFacetGroup
import dev.forcetower.melon.feature.library.domain.model.LibraryReading
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchScope
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchTerm
import dev.forcetower.melon.feature.library.domain.model.LibraryWork
import dev.forcetower.melon.feature.library.domain.model.LibraryWorkType
import dev.forcetower.melon.feature.library.domain.model.LibraryYear
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.MelonBanner
import dev.forcetower.unes.designsystem.foundation.PinnedHeaderHairline
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.library.components.LibraryFreshnessRow
import dev.forcetower.unes.ui.feature.library.components.LibraryRowAvailability
import dev.forcetower.unes.ui.feature.library.components.LibraryShimmerBar
import dev.forcetower.unes.ui.feature.library.components.LibrarySuggestionRow
import dev.forcetower.unes.ui.feature.library.components.LibraryTypeTag
import dev.forcetower.unes.ui.feature.library.components.LibraryWorkMark
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import dev.forcetower.unes.ui.feature.library.LibraryViewModel
import dev.forcetower.unes.ui.feature.library.LibraryIntent
import dev.forcetower.unes.ui.feature.library.LibraryUiState
import dev.forcetower.unes.ui.feature.library.LibrarySearchSession
import dev.forcetower.unes.ui.feature.library.formatLibraryCount
import dev.forcetower.unes.ui.feature.library.formatLibraryAgo
import dev.forcetower.unes.ui.feature.library.labelRes
import dev.forcetower.unes.ui.feature.library.pluralLabelRes
import dev.forcetower.unes.ui.feature.library.facetValueLabel
import dev.forcetower.unes.ui.feature.library.libraryPreviewWork

// Paginated results (dc `BibliotecaScreen` "resultados" scenarios): server
// total + aggregate freshness, degradation banners, sort/facet chips, and the
// edge-to-edge list fed by androidx.paging — each row consults availability
// lazily as it becomes visible. Also owns the "busca ampla" and "nada
// encontrado" guard states and the Refinar sheet.
@Composable
internal fun LibraryResultsScreen(
    query: String,
    scopeWire: String?,
    sessionId: Int,
    onBack: () -> Unit,
    onOpenWork: (workId: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: LibraryViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    // After process death the shared ViewModel is empty — rebuild this
    // route's session from its primary term.
    LaunchedEffect(sessionId) {
        vm.restoreSession(
            sessionId = sessionId,
            query = query,
            scope = LibrarySearchScope.fromWire(scopeWire) ?: LibrarySearchScope.All,
        )
    }

    val pagingItems = remember(sessionId) { vm.pagingWorks(sessionId) }
        .collectAsLazyPagingItems()
    var refineOpen by rememberSaveable { mutableStateOf(false) }

    // Freshness stamps re-render on a slow tick so "há 2 min" stays honest.
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = Clock.System.now()
        }
    }

    val session = state.sessions[sessionId]
    val listState = rememberLazyListState()
    val scrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        ResultsTopBar(
            query = session?.terms?.joinToString(" + ") { it.query } ?: query,
            scope = session?.terms?.firstOrNull()?.scope
                ?: LibrarySearchScope.fromWire(scopeWire)
                ?: LibrarySearchScope.All,
            activeFacetCount = session?.activeFacetCount ?: 0,
            onBack = onBack,
            onOpenRefine = { refineOpen = true },
        )
        PinnedHeaderHairline(scrolled = scrolled)

        when {
            session == null -> Centered { CircularProgressIndicator() }
            session.isTooBroad -> TooBroadState(
                onApplyCentral = {
                    vm.onIntent(
                        LibraryIntent.ToggleFacet(sessionId, LibraryFacetGroup.Branch, "bcjc"),
                    )
                },
                onApplyBooks = {
                    vm.onIntent(
                        LibraryIntent.ToggleFacet(
                            sessionId,
                            LibraryFacetGroup.Type,
                            LibraryWorkType.Book.wire,
                        ),
                    )
                },
                onEditSearch = onBack,
                bottomInset = bottomInset,
            )
            pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0 ->
                RefreshErrorState(onRetry = { pagingItems.retry() })
            pagingItems.loadState.refresh is LoadState.NotLoading &&
                pagingItems.itemCount == 0 && session.activeFacetCount == 0 ->
                EmptyState(
                    query = session.terms.firstOrNull()?.query ?: query,
                    scope = session.terms.firstOrNull()?.scope ?: LibrarySearchScope.All,
                    onSearchAllFields = { vm.broadenScope(sessionId) },
                    onEditSearch = onBack,
                    bottomInset = bottomInset,
                )
            else -> ResultsList(
                vm = vm,
                state = state,
                session = session,
                pagingItems = pagingItems,
                listState = listState,
                now = now,
                onOpenWork = onOpenWork,
                onOpenRefine = { refineOpen = true },
                bottomInset = bottomInset,
            )
        }
    }

    if (refineOpen && session != null) {
        LibraryRefineSheet(
            session = session,
            onDismiss = { refineOpen = false },
            onSetSort = { vm.onIntent(LibraryIntent.SetSort(sessionId, it)) },
            onToggleFacet = { group, key ->
                vm.onIntent(LibraryIntent.ToggleFacet(sessionId, group, key))
            },
            onClearFacets = { vm.onIntent(LibraryIntent.ClearFacets(sessionId)) },
            onSetOnlyAvailable = { vm.onIntent(LibraryIntent.SetOnlyAvailable(sessionId, it)) },
            onSetGroupByType = { vm.onIntent(LibraryIntent.SetGroupByType(sessionId, it)) },
        )
    }
}

// The query pill (back + query/scope + close, all routes back to the entry
// screen) and the tune button carrying the active-filter badge.
@Composable
private fun ResultsTopBar(
    query: String,
    scope: LibrarySearchScope,
    activeFacetCount: Int,
    onBack: () -> Unit,
    onOpenRefine: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp)
            .height(64.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onBack)
                .padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.library_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = query,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.15).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (scope != LibrarySearchScope.All) {
                    Text(
                        text = stringResource(scope.labelRes()).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                        ),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.library_results_edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Box {
            val active = activeFacetCount > 0
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    )
                    .clickable(onClick = onOpenRefine),
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = stringResource(R.string.library_refine_title),
                    tint = if (active) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
            if (active) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.melon.status.bad),
                ) {
                    Text(
                        text = activeFacetCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.melon.fixed.surfaceLight,
                    )
                }
            }
        }
    }
}

// One display entry of the list: a type-group header or a loaded row (its
// paging index keeps append triggering even in grouped/filtered modes).
private sealed interface ResultEntry {
    data class Header(val type: LibraryWorkType, val count: Int) : ResultEntry
    data class Row(val work: LibraryWork, val pagingIndex: Int) : ResultEntry
}

@Composable
private fun ResultsList(
    vm: LibraryViewModel,
    state: LibraryUiState,
    session: LibrarySearchSession,
    pagingItems: LazyPagingItems<LibraryWork>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    now: Instant,
    onOpenWork: (String, String) -> Unit,
    onOpenRefine: () -> Unit,
    bottomInset: Dp,
) {
    val snapshot = pagingItems.itemSnapshotList.items

    fun effectiveAvailability(work: LibraryWork): LibraryAvailability {
        val copies = state.liveCopies[work.id] ?: work.copies
        return work.copy(copies = copies).availability(now)
    }

    // "Só com exemplar livre" filters what has already been read — a row only
    // survives once its consultation came back with a free copy.
    val entries = remember(
        snapshot, session.groupByType, session.onlyAvailable, state.readings, state.liveCopies, now,
    ) {
        val visible = snapshot.withIndex().filter { (_, work) ->
            if (!session.onlyAvailable) return@filter true
            val reading = state.readings[work.id]
            reading is LibraryReading.Fresh || reading is LibraryReading.Stale
        }.filter { (_, work) ->
            !session.onlyAvailable ||
                work.copy(copies = state.liveCopies[work.id] ?: work.copies)
                    .availability(now).available > 0
        }
        if (!session.groupByType) {
            visible.map { (index, work) -> ResultEntry.Row(work, index) }
        } else {
            LibraryWorkType.entries.flatMap { type ->
                val group = visible.filter { (_, work) -> work.type == type }
                if (group.isEmpty()) {
                    emptyList()
                } else {
                    listOf(ResultEntry.Header(type, group.size)) +
                        group.map { (index, work) -> ResultEntry.Row(work, index) }
                }
            }
        }
    }

    // The screen's aggregate reading: down beats stale beats fresh; null
    // while the first consultations are still in flight.
    val aggregate: LibraryReading? = remember(snapshot, state.readings) {
        val readings = snapshot.mapNotNull { state.readings[it.id] }
        when {
            readings.isEmpty() && snapshot.isNotEmpty() -> null
            readings.any { it == LibraryReading.Unavailable } -> LibraryReading.Unavailable
            else -> readings.firstOrNull { it is LibraryReading.Stale } ?: readings.firstOrNull()
        }
    }

    val total = session.total
    val loading = pagingItems.loadState.refresh is LoadState.Loading
    val appendLoading = pagingItems.loadState.append is LoadState.Loading
    val appendError = pagingItems.loadState.append is LoadState.Error
    val endReached = pagingItems.loadState.append.endOfPaginationReached &&
        pagingItems.loadState.refresh is LoadState.NotLoading

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            bottom = bottomInset + 40.dp,
        ),
    ) {
        item(key = "header") {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                if (total != null) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = formatLibraryCount(total),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                letterSpacing = (-0.84).sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = if (session.activeFacetCount > 0) {
                                pluralStringResource(
                                    R.plurals.library_results_works_filtered,
                                    total,
                                )
                            } else {
                                pluralStringResource(
                                    R.plurals.library_results_works_for,
                                    total,
                                    session.terms.joinToString(" + ") { it.query },
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                } else {
                    LibraryShimmerBar(width = 180, height = 22)
                }
                LibraryFreshnessRow(
                    reading = aggregate,
                    checking = state.refreshing,
                    now = now,
                    onRefresh = {
                        vm.onIntent(LibraryIntent.RefreshReadings(snapshot.map { it.id }))
                    },
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        if (aggregate == LibraryReading.Unavailable || aggregate is LibraryReading.Stale) {
            item(key = "banner") {
                val down = aggregate == LibraryReading.Unavailable
                MelonBanner(
                    title = if (down) {
                        stringResource(R.string.library_fresh_down)
                    } else {
                        stringResource(
                            R.string.library_fresh_stale,
                            formatLibraryAgo((aggregate as LibraryReading.Stale).checkedAt, now),
                        )
                    },
                    detail = if (down) {
                        stringResource(R.string.library_banner_down)
                    } else {
                        stringResource(R.string.library_banner_stale)
                    },
                    tone = if (down) {
                        MaterialTheme.melon.status.bad
                    } else {
                        MaterialTheme.melon.status.warn
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
        }

        item(key = "chips") {
            SortAndFacetChips(
                session = session,
                onOpenRefine = onOpenRefine,
                onRemoveFacet = { group, key ->
                    vm.onIntent(LibraryIntent.ToggleFacet(session.id, group, key))
                },
                onClear = { vm.onIntent(LibraryIntent.ClearFacets(session.id)) },
            )
        }

        if (loading && pagingItems.itemCount == 0) {
            items(count = 4, key = { "skeleton-$it" }) {
                SkeletonRow(opacity = 1f - it * 0.22f)
            }
        } else if (
            entries.isEmpty() && !loading && !appendLoading &&
            (session.onlyAvailable || session.activeFacetCount > 0)
        ) {
            // Refine dead-end: the query has results, the filters hide all of
            // them (or nothing available survived the availability filter).
            item(key = "refine-dead-end") {
                // Touching the last loaded index keeps append moving even
                // though no row is rendered — later pages may still contain
                // works that pass the filter (same as the iOS sentinel).
                if (pagingItems.itemCount > 0 && !endReached) {
                    pagingItems[pagingItems.itemCount - 1]
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 60.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        text = stringResource(R.string.library_results_filtered_empty),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    TextButton(
                        onClick = {
                            vm.onIntent(LibraryIntent.ClearFacets(session.id))
                            vm.onIntent(LibraryIntent.SetOnlyAvailable(session.id, false))
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(text = stringResource(R.string.library_filters_clear))
                    }
                }
            }
        } else {
            entries.forEach { entry ->
                when (entry) {
                    is ResultEntry.Header -> item(key = "type-${entry.type.wire}") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = 18.dp,
                                bottom = 8.dp,
                            ),
                        ) {
                            Text(
                                text = stringResource(entry.type.pluralLabelRes()).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.7.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = entry.count.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    ),
                            )
                        }
                    }
                    is ResultEntry.Row -> item(key = entry.work.id) {
                        // The get() keeps androidx.paging's prefetch window
                        // moving even when grouping/filtering reorders rows.
                        pagingItems[entry.pagingIndex]
                        WorkRow(
                            work = entry.work,
                            reading = state.readings[entry.work.id],
                            availability = effectiveAvailability(entry.work),
                            now = now,
                            onShown = { vm.onIntent(LibraryIntent.RowShown(entry.work.id)) },
                            onTap = {
                                vm.openWork(entry.work)
                                onOpenWork(entry.work.id, entry.work.parsedTitle.title)
                            },
                        )
                    }
                }
            }

            if (appendLoading) {
                items(count = 3, key = { "append-skeleton-$it" }) {
                    SkeletonRow(opacity = 1f - it * 0.3f)
                }
            }
            if (appendError) {
                item(key = "append-error") {
                    TextButton(
                        onClick = { pagingItems.retry() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        Text(text = stringResource(R.string.library_results_append_retry))
                    }
                }
            }

            item(key = "footer") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                ) {
                    if (!endReached && !appendError) {
                        LinearProgressIndicator(modifier = Modifier.width(140.dp))
                    }
                    val loaded = snapshot.size
                    Text(
                        text = when {
                            endReached -> pluralStringResource(
                                R.plurals.library_results_end,
                                loaded,
                                formatLibraryCount(loaded),
                            )
                            total != null -> stringResource(
                                R.string.library_results_loaded_format,
                                formatLibraryCount(loaded),
                                formatLibraryCount(total),
                            )
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun SortAndFacetChips(
    session: LibrarySearchSession,
    onOpenRefine: () -> Unit,
    onRemoveFacet: (LibraryFacetGroup, String) -> Unit,
    onClear: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.melon.surface.card)
                .border(1.dp, MaterialTheme.melon.surface.line, RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenRefine)
                .padding(horizontal = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.SwapVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(session.sort.labelRes()),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        session.facets.forEach { (group, keys) ->
            keys.sorted().forEach { key ->
                val value = session.serverFacets[group]?.firstOrNull { it.key == key }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(onClick = { onRemoveFacet(group, key) })
                        .padding(horizontal = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = value?.let { facetValueLabel(group, it) } ?: key,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.library_facet_remove),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
        if (session.activeFacetCount > 1) {
            TextButton(onClick = onClear) {
                Text(
                    text = stringResource(R.string.library_filters_clear),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

// One catalogue row: spine mark, type/year/edition strip, title, subtitle,
// authors, and the availability line. `onShown` fires the lazy consultation.
@Composable
private fun WorkRow(
    work: LibraryWork,
    reading: LibraryReading?,
    availability: LibraryAvailability,
    now: Instant,
    onShown: () -> Unit,
    onTap: () -> Unit,
) {
    LaunchedEffect(work.id) { onShown() }
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            LibraryWorkMark(work = work)
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LibraryTypeTag(work = work)
                    val year = work.year
                    Text(
                        text = when (year) {
                            is LibraryYear.Year -> year.text
                            is LibraryYear.Illegible ->
                                stringResource(R.string.library_year_illegible)
                            LibraryYear.None -> ""
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.outline,
                    )
                    work.edition?.let { edition ->
                        Text(
                            text = "· $edition",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = work.parsedTitle.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.39).sp,
                        lineHeight = 19.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                work.parsedTitle.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = work.authors.takeIf { it.isNotEmpty() }
                        ?.let { authors ->
                            if (authors.size <= 2) {
                                authors.joinToString(" · ")
                            } else {
                                authors.take(2).joinToString(" · ") + " +${authors.size - 2}"
                            }
                        }
                        ?: stringResource(R.string.library_author_unknown),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(modifier = Modifier.padding(top = 3.dp)) {
                    LibraryRowAvailability(
                        work = work,
                        reading = reading,
                        effectiveAvailability = availability,
                        now = now,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        )
    }
}

@Composable
private fun SkeletonRow(opacity: Float) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        LibraryShimmerBar(width = 56, height = 80)
        Column(
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(top = 5.dp),
        ) {
            LibraryShimmerBar(width = 70, height = 9)
            LibraryShimmerBar(width = 240, height = 13)
            LibraryShimmerBar(width = 150, height = 10)
            LibraryShimmerBar(width = 120, height = 9)
        }
    }
}

@Composable
private fun TooBroadState(
    onApplyCentral: () -> Unit,
    onApplyBooks: () -> Unit,
    onEditSearch: () -> Unit,
    bottomInset: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomInset),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.melon.status.warn.copy(alpha = 0.16f)),
            ) {
                Icon(
                    imageVector = Icons.Filled.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.melon.status.warn,
                    modifier = Modifier.size(34.dp),
                )
            }
            Text(
                text = stringResource(R.string.library_broad_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    letterSpacing = (-0.6).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = stringResource(R.string.library_broad_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            LibrarySuggestionRow(
                icon = Icons.AutoMirrored.Filled.ManageSearch,
                label = stringResource(R.string.library_broad_author),
                hint = stringResource(R.string.library_broad_author_hint),
                onTap = onEditSearch,
            )
            LibrarySuggestionRow(
                icon = Icons.Filled.LocationOn,
                label = stringResource(R.string.library_broad_central),
                hint = stringResource(R.string.library_broad_central_hint),
                onTap = onApplyCentral,
            )
            LibrarySuggestionRow(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = stringResource(R.string.library_broad_books),
                hint = stringResource(R.string.library_broad_books_hint),
                onTap = onApplyBooks,
            )
        }
    }
}

@Composable
private fun EmptyState(
    query: String,
    scope: LibrarySearchScope,
    onSearchAllFields: () -> Unit,
    onEditSearch: () -> Unit,
    bottomInset: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomInset),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(34.dp),
                )
            }
            Text(
                text = stringResource(R.string.library_empty_title, query),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    letterSpacing = (-0.6).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = stringResource(R.string.library_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (scope != LibrarySearchScope.All) {
                LibrarySuggestionRow(
                    icon = Icons.Filled.Search,
                    label = stringResource(R.string.library_empty_all_fields),
                    hint = stringResource(R.string.library_empty_all_fields_hint),
                    onTap = onSearchAllFields,
                )
            }
            LibrarySuggestionRow(
                icon = Icons.AutoMirrored.Filled.ManageSearch,
                label = stringResource(R.string.library_empty_edit),
                hint = stringResource(R.string.library_empty_edit_hint),
                onTap = onEditSearch,
            )
        }
    }
}

@Composable
private fun RefreshErrorState(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 90.dp),
    ) {
        Text(
            text = stringResource(R.string.library_error_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.library_error_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            Text(text = stringResource(R.string.library_error_retry))
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
    ) {
        content()
    }
}

@Preview
@Composable
private fun LibraryResultsScreenPreview() {
    MelonTheme {
        val work = libraryPreviewWork()
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            ResultsTopBar(
                query = "cálculo",
                scope = LibrarySearchScope.All,
                activeFacetCount = 1,
                onBack = {},
                onOpenRefine = {},
            )
            WorkRow(
                work = work,
                reading = LibraryReading.Fresh(checkedAt = Clock.System.now()),
                availability = work.availability(Clock.System.now()),
                now = Clock.System.now(),
                onShown = {},
                onTap = {},
            )
            SkeletonRow(opacity = 0.6f)
        }
    }
}
