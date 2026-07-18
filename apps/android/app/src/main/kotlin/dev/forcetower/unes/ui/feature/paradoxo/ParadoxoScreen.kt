package dev.forcetower.unes.ui.feature.paradoxo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoExploreKind
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoIndexEntry
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoMyDiscipline
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoOverview
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoPulseFact
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoPulseKind
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoRef
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoTier
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.PinnedHeaderHairline
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoCard
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoFailure
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoListRow
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoLoading
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoPulseHero
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoSparkline
import dev.forcetower.unes.ui.feature.paradoxo.components.paradoxoTierLabel
import dev.forcetower.unes.ui.feature.paradoxo.components.paradoxoTone

// Paradoxo home — the university-wide grade explorer (dc `ParadoxoScreen`,
// iOS `ParadoxoView`). Header + rotating pulse hero + explore grid + the
// student's own disciplines, with an inline search mode over the index.
@Composable
internal fun ParadoxoScreen(
    onBack: () -> Unit,
    onOpenDiscipline: (id: String, name: String) -> Unit,
    onOpenTeacher: (id: String, name: String) -> Unit,
    onOpenExplore: (ParadoxoExploreKind) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: ParadoxoViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.onIntent(ParadoxoIntent.Load) }

    var searching by rememberSaveable { mutableStateOf(false) }
    val closeSearch = {
        searching = false
        vm.onIntent(ParadoxoIntent.QueryChanged(""))
    }
    BackHandler(enabled = searching) { closeSearch() }

    val openRef: (ParadoxoRef, String) -> Unit = { ref, name ->
        vm.trackEntityOpen(ref)
        when (ref) {
            is ParadoxoRef.Discipline -> onOpenDiscipline(ref.id, name)
            is ParadoxoRef.Teacher -> onOpenTeacher(ref.id, name)
        }
    }
    val trackedOpenExplore: (ParadoxoExploreKind) -> Unit = { kind ->
        vm.trackExploreOpen(kind)
        onOpenExplore(kind)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        if (searching) {
            ParadoxoSearchContent(
                state = state,
                onQueryChanged = { vm.onIntent(ParadoxoIntent.QueryChanged(it)) },
                onCancel = closeSearch,
                onOpen = openRef,
                bottomInset = bottomInset,
            )
        } else {
            ParadoxoHomeContent(
                state = state,
                onBack = onBack,
                onRetry = { vm.onIntent(ParadoxoIntent.Retry) },
                onOpenSearch = { searching = true },
                onOpen = openRef,
                onOpenExplore = trackedOpenExplore,
                bottomInset = bottomInset,
            )
        }
    }
}

@Composable
private fun ParadoxoHomeContent(
    state: ParadoxoUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpen: (ParadoxoRef, String) -> Unit,
    onOpenExplore: (ParadoxoExploreKind) -> Unit,
    bottomInset: Dp,
) {
    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 0 } }

    // The app bar stays pinned; the headline and the explorer scroll
    // beneath it.
    Column(modifier = Modifier.fillMaxSize()) {
        ParadoxoHomeBar(
            onBack = onBack,
            avatarInitial = state.avatarInitial,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .fadeUpOnAppear(delayMs = 20),
        )
        PinnedHeaderHairline(scrolled = scrolled)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = bottomInset),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fadeUpOnAppear(delayMs = 40),
            ) {
                Text(
                    text = stringResource(R.string.paradoxo_eyebrow).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.paradoxo_title),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.64).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = stringResource(R.string.paradoxo_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .widthIn(max = 280.dp),
                )
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                // Search pill — tapping swaps the screen into search mode.
                Row(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(role = Role.Button, onClick = onOpenSearch)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fadeUpOnAppear(delayMs = 80),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.paradoxo_search_prompt),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }

                val overview = state.overview
                when {
                    overview != null -> ParadoxoPulseHero(
                        facts = overview.pulse,
                        onOpen = { ref -> onOpen(ref, "") },
                        modifier = Modifier
                            .padding(top = 18.dp)
                            .scaleInOnAppear(delayMs = 120, fromScale = 0.97f),
                    )
                    state.failed -> ParadoxoFailure(onRetry = onRetry)
                    else -> ParadoxoLoading()
                }

                ParadoxoExploreGrid(
                    onOpenExplore = onOpenExplore,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fadeUpOnAppear(delayMs = 200),
                )

                if (overview != null && overview.myDisciplines.isNotEmpty()) {
                    Column(modifier = Modifier.fadeUpOnAppear(delayMs = 260)) {
                        Text(
                            text = stringResource(R.string.paradoxo_section_mine).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.44.sp,
                            ),
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 26.dp),
                        )
                        Text(
                            text = stringResource(R.string.paradoxo_section_mine_note),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(top = 3.dp, bottom = 14.dp),
                        )
                        ParadoxoCard {
                            overview.myDisciplines.forEachIndexed { index, mine ->
                                ParadoxoListRow(
                                    mean = mine.mean,
                                    title = mine.name,
                                    subtitle = mineSubtitle(mine.sampleCount, mine.myPercentile),
                                    onClick = {
                                        onOpen(ParadoxoRef.Discipline(mine.id), mine.name)
                                    },
                                    tileSize = 46.dp,
                                    showDivider = index > 0,
                                    trailing = if (mine.spark.size >= 2) {
                                        {
                                            ParadoxoSparkline(
                                                values = mine.spark.takeLast(8),
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                                modifier = Modifier.size(width = 42.dp, height = 18.dp),
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }
                }

                if (overview != null && overview.studentCount > 0) {
                    Text(
                        text = stringResource(
                            R.string.paradoxo_footer_format,
                            ParadoxoFormat.count(overview.studentCount),
                            ParadoxoFormat.count(overview.meanCount),
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ParadoxoHomeBar(
    onBack: () -> Unit,
    avatarInitial: String?,
    modifier: Modifier = Modifier,
) {
    val backLabel = stringResource(R.string.paradoxo_back)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 6.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = backLabel, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = backLabel,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp),
            )
        }
        if (avatarInitial != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.melon.palette.teal,
                                MaterialTheme.melon.palette.violet,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = avatarInitial,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.melon.fixed.onHero,
                )
            }
        }
    }
}

// ───────── Explore grid ─────────

private data class ExploreEntry(
    val kind: ParadoxoExploreKind,
    val icon: ImageVector,
    val titleRes: Int,
    val subRes: Int,
)

private val ExploreEntries = listOf(
    ExploreEntry(
        kind = ParadoxoExploreKind.Brutal,
        icon = Icons.Filled.Bolt,
        titleRes = R.string.paradoxo_explore_brutal_title,
        subRes = R.string.paradoxo_explore_brutal_sub,
    ),
    ExploreEntry(
        kind = ParadoxoExploreKind.Kind,
        icon = Icons.Filled.WbSunny,
        titleRes = R.string.paradoxo_explore_kind_title,
        subRes = R.string.paradoxo_explore_kind_sub,
    ),
    ExploreEntry(
        kind = ParadoxoExploreKind.Rising,
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        titleRes = R.string.paradoxo_explore_rising_title,
        subRes = R.string.paradoxo_explore_rising_sub,
    ),
    ExploreEntry(
        kind = ParadoxoExploreKind.Gap,
        icon = Icons.Filled.Contrast,
        titleRes = R.string.paradoxo_explore_gap_title,
        subRes = R.string.paradoxo_explore_gap_sub,
    ),
)

@Composable
internal fun paradoxoExploreTone(kind: ParadoxoExploreKind): Color = when (kind) {
    ParadoxoExploreKind.Brutal -> MaterialTheme.melon.status.bad
    ParadoxoExploreKind.Kind -> MaterialTheme.melon.status.ok
    ParadoxoExploreKind.Rising -> MaterialTheme.melon.palette.teal
    ParadoxoExploreKind.Gap -> MaterialTheme.melon.palette.magenta
}

@Composable
private fun ParadoxoExploreGrid(
    onOpenExplore: (ParadoxoExploreKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.paradoxo_section_explore).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.44.sp,
            ),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        ExploreEntries.chunked(2).forEachIndexed { rowIndex, rowEntries ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (rowIndex > 0) 12.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowEntries.forEach { entry ->
                    ExploreCard(
                        entry = entry,
                        onClick = { onOpenExplore(entry.kind) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreCard(
    entry: ExploreEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone = paradoxoExploreTone(entry.kind)
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        // Tonal glow bleeding from the top-right corner (dc `glowStyle`).
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-24).dp)
                .size(74.dp)
                .clip(CircleShape)
                .background(tone.copy(alpha = 0.13f)),
        )
        Column(modifier = Modifier.padding(15.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(tone.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = stringResource(entry.titleRes),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = stringResource(entry.subRes),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

// ───────── Search mode ─────────

@Composable
private fun ParadoxoSearchContent(
    state: ParadoxoUiState,
    onQueryChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onOpen: (ParadoxoRef, String) -> Unit,
    bottomInset: Dp,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(18.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (state.query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.paradoxo_search_prompt),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    BasicTextField(
                        value = state.query,
                        onValueChange = onQueryChanged,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                }
            }
            Text(
                text = stringResource(R.string.paradoxo_search_cancel),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onCancel)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
            )
        }

        val query = state.query.trim()
        val results = remember(state.index, query) { searchParadoxo(state.index, query) }
        val suggestions = remember(state.index) {
            ParadoxoSearchResults(
                disciplines = state.index
                    .filter { it.entry.ref is ParadoxoRef.Discipline }
                    .take(3)
                    .map { it.entry },
                teachers = state.index
                    .filter { it.entry.ref is ParadoxoRef.Teacher }
                    .take(4)
                    .map { it.entry },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = bottomInset + 28.dp),
        ) {
            when {
                query.isEmpty() -> {
                    SearchGroup(
                        label = stringResource(R.string.paradoxo_search_suggestions),
                        entries = suggestions.disciplines,
                        onOpen = onOpen,
                    )
                    Spacer(Modifier.height(16.dp))
                    SearchGroup(
                        label = stringResource(R.string.paradoxo_search_teachers),
                        entries = suggestions.teachers,
                        onOpen = onOpen,
                    )
                }
                results.isEmpty -> SearchEmpty(query = query)
                else -> {
                    if (results.disciplines.isNotEmpty()) {
                        SearchGroup(
                            label = stringResource(
                                R.string.paradoxo_search_section_count_format,
                                stringResource(R.string.paradoxo_search_disciplines),
                                results.disciplines.size,
                            ),
                            entries = results.disciplines,
                            onOpen = onOpen,
                        )
                    }
                    if (results.teachers.isNotEmpty()) {
                        if (results.disciplines.isNotEmpty()) Spacer(Modifier.height(16.dp))
                        SearchGroup(
                            label = stringResource(
                                R.string.paradoxo_search_section_count_format,
                                stringResource(R.string.paradoxo_search_teachers),
                                results.teachers.size,
                            ),
                            entries = results.teachers,
                            onOpen = onOpen,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchGroup(
    label: String,
    entries: List<ParadoxoIndexEntry>,
    onOpen: (ParadoxoRef, String) -> Unit,
) {
    if (entries.isEmpty()) return
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
    ParadoxoCard(cornerRadius = 20.dp) {
        entries.forEachIndexed { index, entry ->
            ParadoxoListRow(
                mean = entry.mean,
                title = entry.name,
                subtitle = indexSubtitle(entry),
                onClick = { onOpen(entry.ref, entry.name) },
                showDivider = index > 0,
            )
        }
    }
}

@Composable
private fun SearchEmpty(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = stringResource(R.string.paradoxo_search_empty_format, query),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

// ───────── Row subtitles ─────────

@Composable
private fun mineSubtitle(sampleCount: Int, myPercentile: Int?): AnnotatedString {
    val ok = MaterialTheme.melon.status.ok
    val samples = stringResource(
        R.string.paradoxo_samples_format,
        ParadoxoFormat.count(sampleCount),
    )
    return buildAnnotatedString {
        append(samples)
        val percentile = myPercentile ?: return@buildAnnotatedString
        append(" · ")
        withStyle(SpanStyle(color = ok, fontWeight = FontWeight.Bold)) {
            append(
                stringResource(
                    R.string.paradoxo_top_percent_format,
                    ParadoxoFormat.percent(100 - percentile),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun ParadoxoScreenPreview() {
    MelonTheme {
        ParadoxoHomeContent(
            state = ParadoxoUiState(
                overview = ParadoxoOverview(
                    pulse = listOf(
                        ParadoxoPulseFact(
                            id = "p1",
                            kind = ParadoxoPulseKind.Brutal,
                            metric = 3.5,
                            title = "Cálculo Diferencial e Integral I",
                            subtitle = "60% reprovam · 6.868 alunos",
                            ref = ParadoxoRef.Discipline("d1"),
                        ),
                    ),
                    myDisciplines = listOf(
                        ParadoxoMyDiscipline(
                            id = "d1",
                            code = "EXA704",
                            name = "Cálculo Diferencial e Integral I",
                            mean = 3.5,
                            sampleCount = 6868,
                            spark = listOf(4.0, 3.5, 3.1, 2.9, 3.3, 3.5),
                            myPercentile = 8,
                        ),
                        ParadoxoMyDiscipline(
                            id = "d3",
                            code = "EXA805",
                            name = "Algoritmos e Programação II",
                            mean = 5.7,
                            sampleCount = 1079,
                            spark = listOf(6.2, 6.5, 5.8, 5.3, 5.5, 5.7),
                            myPercentile = null,
                        ),
                    ),
                    rankings = emptyList(),
                    studentCount = 42318,
                    meanCount = 3912,
                ),
                avatarInitial = "J",
            ),
            onBack = {},
            onRetry = {},
            onOpenSearch = {},
            onOpen = { _, _ -> },
            onOpenExplore = {},
            bottomInset = 0.dp,
        )
    }
}

@Composable
private fun indexSubtitle(entry: ParadoxoIndexEntry): AnnotatedString {
    return when (entry.ref) {
        is ParadoxoRef.Discipline -> buildAnnotatedString {
            val code = entry.code
            if (!code.isNullOrBlank()) {
                append(code)
                append(" · ")
            }
            append(
                stringResource(
                    R.string.paradoxo_samples_format,
                    ParadoxoFormat.count(entry.studentCount),
                ),
            )
        }
        is ParadoxoRef.Teacher -> buildAnnotatedString {
            append(
                stringResource(
                    R.string.paradoxo_students_format,
                    ParadoxoFormat.count(entry.studentCount),
                ),
            )
            append(" · ")
            withStyle(
                SpanStyle(color = paradoxoTone(entry.mean), fontWeight = FontWeight.Bold),
            ) {
                append(paradoxoTierLabel(ParadoxoTier.of(entry.mean)))
            }
        }
    }
}
