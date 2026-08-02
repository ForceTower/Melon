package dev.forcetower.unes.ui.feature.library.search

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.library.domain.model.LibraryFacetSelection
import dev.forcetower.melon.feature.library.domain.model.LibraryRecentSearch
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchScope
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchTerm
import dev.forcetower.melon.feature.library.domain.model.LibraryWork
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.PinnedHeaderHairline
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.ui.feature.library.components.LibraryBackButton
import dev.forcetower.unes.ui.feature.library.components.LibraryCard
import dev.forcetower.unes.ui.feature.library.components.LibraryFilterChip
import dev.forcetower.unes.ui.feature.library.components.LibraryInfoNote
import dev.forcetower.unes.ui.feature.library.components.LibrarySectionLabel
import dev.forcetower.unes.ui.feature.library.components.LibraryTypeTag
import dev.forcetower.unes.ui.feature.library.components.LibraryWorkMark
import dev.forcetower.unes.ui.feature.library.LibraryViewModel
import dev.forcetower.unes.ui.feature.library.LibraryIntent
import dev.forcetower.unes.ui.feature.library.formatLibraryCount
import dev.forcetower.unes.ui.feature.library.labelRes
import dev.forcetower.unes.ui.feature.library.libraryPreviewWork

// Biblioteca search entry (dc `BibliotecaScreen` "busca" scenario): docked
// search bar with scope chips, the advanced-search entry, the server-kept
// recent searches, and the "novas no acervo" shelf (degraded record shape —
// availability is consulted when a work opens). Pushed from the "Biblioteca"
// shortcut on the Me hub.
@Composable
internal fun LibraryScreen(
    onBack: () -> Unit,
    onOpenResults: (query: String, scope: String?, sessionId: Int) -> Unit,
    onOpenWork: (workId: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: LibraryViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.onIntent(LibraryIntent.LoadOverview) }

    var query by rememberSaveable { mutableStateOf("") }
    var scope by rememberSaveable { mutableStateOf(LibrarySearchScope.All.wire) }
    var advancedOpen by rememberSaveable { mutableStateOf(false) }
    val selectedScope = LibrarySearchScope.fromWire(scope) ?: LibrarySearchScope.All

    val submit: (List<LibrarySearchTerm>, LibraryFacetSelection) -> Unit =
        { terms, facets ->
            val sessionId = vm.startSearch(terms, facets)
            advancedOpen = false
            if (sessionId != 0) {
                val first = terms.first()
                onOpenResults(
                    terms.joinToString(" + ") { it.query },
                    first.scope.takeIf { it != LibrarySearchScope.All }?.wire,
                    sessionId,
                )
            }
        }

    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 0 } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp)
                .height(48.dp),
        ) {
            LibraryBackButton(onBack = onBack)
        }
        PinnedHeaderHairline(scrolled = scrolled)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = bottomInset + 24.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = stringResource(R.string.library_eyebrow).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 20),
                )
                Text(
                    text = stringResource(R.string.library_title),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        letterSpacing = (-0.64).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fadeUpOnAppear(delayMs = 40),
                )
                Text(
                    text = stringResource(R.string.library_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fadeUpOnAppear(delayMs = 70),
                )

                LibrarySearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onSubmit = {
                        if (query.isNotBlank()) {
                            submit(listOf(LibrarySearchTerm(query, selectedScope)), emptyMap())
                        }
                    },
                    onOpenAdvanced = { advancedOpen = true },
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .fadeUpOnAppear(delayMs = 100),
                )
            }

            // Scope chips — bleed to the screen edge like the M3 spec row.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .fadeUpOnAppear(delayMs = 130),
            ) {
                LibrarySearchScope.entries.forEach { entry ->
                    LibraryFilterChip(
                        selected = entry == selectedScope,
                        onClick = { scope = entry.wire },
                        label = stringResource(entry.labelRes()),
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                AdvancedSearchEntryCard(
                    onTap = { advancedOpen = true },
                    modifier = Modifier.fadeUpOnAppear(delayMs = 160),
                )
            }

            val overview = state.overview
            when {
                overview != null -> {
                    val recents = overview.recents.takeUnless { state.recentsCleared }.orEmpty()
                    if (recents.isNotEmpty()) {
                        RecentsSection(
                            recents = recents,
                            onClear = { vm.onIntent(LibraryIntent.ClearRecents) },
                            onTap = { recent ->
                                submit(
                                    listOf(LibrarySearchTerm(recent.query, recent.scope)),
                                    emptyMap(),
                                )
                            },
                            modifier = Modifier.fadeUpOnAppear(delayMs = 200),
                        )
                    }
                    if (overview.newAcquisitions.isNotEmpty()) {
                        NewAcquisitionsSection(
                            works = overview.newAcquisitions,
                            onTap = { work ->
                                vm.openWork(work)
                                onOpenWork(work.id, work.parsedTitle.title)
                            },
                            modifier = Modifier.fadeUpOnAppear(delayMs = 240),
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(top = 20.dp)
                            .fadeUpOnAppear(delayMs = 280),
                    ) {
                        LibraryInfoNote(
                            text = stringResource(R.string.library_covers_note),
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                        )
                    }
                }
                state.overviewFailed -> OverviewError(
                    onRetry = { vm.onIntent(LibraryIntent.RetryOverview) },
                )
                state.overviewLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (advancedOpen) {
        LibraryAdvancedSheet(
            initialQuery = query,
            onDismiss = { advancedOpen = false },
            onSubmit = submit,
        )
    }
}

// M3 docked search bar: leading glyph, clear affordance, and the tune button
// that opens the advanced sheet.
@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenAdvanced: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 16.dp, end = 6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.library_search_placeholder),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.library_search_clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onOpenAdvanced) {
            Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = stringResource(R.string.library_advanced_entry_title),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AdvancedSearchEntryCard(onTap: () -> Unit, modifier: Modifier = Modifier) {
    LibraryCard(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ManageSearch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.library_advanced_entry_title),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.15).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.library_advanced_entry_hint),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// Server-kept recent searches — edge-to-edge M3 list rows with the result
// count on the trailing edge.
@Composable
private fun RecentsSection(
    recents: List<LibraryRecentSearch>,
    onClear: () -> Unit,
    onTap: (LibraryRecentSearch) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 26.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 8.dp),
        ) {
            LibrarySectionLabel(
                text = stringResource(R.string.library_recents_label),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear) {
                Text(
                    text = stringResource(R.string.library_recents_clear),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        recents.forEachIndexed { index, recent ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onTap(recent) })
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recent.query,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (recent.scope != LibrarySearchScope.All) {
                        Text(
                            text = stringResource(
                                R.string.library_recents_scope_format,
                                stringResource(recent.scope.labelRes()).lowercase(),
                            ),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Text(
                    text = formatLibraryCount(recent.resultCount),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

// "Novas no acervo" — the degraded record shape on a horizontal rail; the
// availability consultation happens when the work opens.
@Composable
private fun NewAcquisitionsSection(
    works: List<LibraryWork>,
    onTap: (LibraryWork) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 26.dp)) {
        LibrarySectionLabel(
            text = stringResource(R.string.library_new_label),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            works.forEach { work ->
                NewAcquisitionCard(work = work, onTap = { onTap(work) })
            }
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            LibraryInfoNote(
                text = stringResource(R.string.library_new_note),
                icon = Icons.Filled.Info,
            )
        }
    }
}

@Composable
private fun NewAcquisitionCard(
    work: LibraryWork,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LibraryCard(modifier = modifier.width(216.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .clickable(onClick = onTap)
                .padding(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LibraryWorkMark(work = work, width = 46, height = 66)
                Column(modifier = Modifier.weight(1f)) {
                    LibraryTypeTag(work = work)
                    Text(
                        text = work.parsedTitle.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 17.sp,
                            letterSpacing = (-0.28).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            Text(
                text = work.authors.firstOrNull()
                    ?: stringResource(R.string.library_author_unknown),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = stringResource(R.string.library_new_availability),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun OverviewError(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 60.dp),
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

@Preview
@Composable
private fun LibraryScreenPreview() {
    MelonTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
        ) {
            LibrarySearchField(
                query = "",
                onQueryChange = {},
                onSubmit = {},
                onOpenAdvanced = {},
            )
            AdvancedSearchEntryCard(onTap = {})
            NewAcquisitionCard(work = libraryPreviewWork(), onTap = {})
        }
    }
}
