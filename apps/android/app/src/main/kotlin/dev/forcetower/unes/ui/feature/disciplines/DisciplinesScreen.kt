package dev.forcetower.unes.ui.feature.disciplines

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.components.AttentionBanner
import dev.forcetower.unes.ui.feature.disciplines.components.DisciplineCard
import dev.forcetower.unes.ui.feature.disciplines.components.HistorySemesterCard
import dev.forcetower.unes.ui.feature.disciplines.components.HistorySummaryCard
import dev.forcetower.unes.ui.feature.disciplines.components.UndownloadedSemesterCard
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// "Disciplinas" tab — 2026 redesign (dc project `UNES Disciplinas - Android`):
// M3 large-style app bar with the semester eyebrow, primary tabs
// Atual · Histórico backed by a swipeable pager, tonal course cards with the
// média ring on Atual, and the accumulated-performance card + semester
// accordions (+ tap-to-download placeholders) on Histórico. Consumes the same
// KMP feed as before (`ObserveDisciplinesListUseCase`, `SyncSemesterUseCase`)
// through `DisciplinesListViewModel`.
@Composable
internal fun DisciplinesScreen(
    onOpenDiscipline: (Discipline) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: DisciplinesListViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val palette = MaterialTheme.melon.palette

    // Color resolution depends on the palette CompositionLocal, so we tint the
    // KMP-mapped projection here (the ViewModel left `color = Unspecified`).
    val current = remember(state.current, palette) { state.current?.tinted(palette) }
    val past = remember(state.past, palette) { state.past.map { it.tinted(palette) } }

    // Between semesters, upstream returns no running semester — promote the
    // most-recent past into the Atual tab so it still opens with a meaningful
    // card stack. The promoted semester keeps its place on the Histórico tab,
    // which always lists every downloaded past semester.
    val effectiveCurrent = current ?: past.firstOrNull()

    DisciplinesContent(
        current = effectiveCurrent,
        isCurrentLive = current != null,
        past = past,
        pending = state.pending,
        downloading = state.downloading,
        overallScore = state.overallScore,
        onOpenDiscipline = onOpenDiscipline,
        onDownload = { vm.onIntent(DisciplinesIntent.Download(it)) },
        bottomInset = bottomInset,
        modifier = modifier,
    )
}

private const val PAGE_CURRENT = 0
private const val PAGE_HISTORY = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisciplinesContent(
    current: Semester?,
    isCurrentLive: Boolean,
    past: List<Semester>,
    pending: List<Semester>,
    downloading: Set<String>,
    overallScore: Double?,
    onOpenDiscipline: (Discipline) -> Unit,
    onDownload: (String) -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Header(
            semesterCode = current?.id,
            isLive = isCurrentLive,
            modifier = Modifier.fadeUpOnAppear(delayMs = 60, fromOffset = (-10).dp),
        )
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            divider = { HorizontalDivider(color = MaterialTheme.melon.surface.line) },
            modifier = Modifier.fadeUpOnAppear(delayMs = 120, fromOffset = (-8).dp),
        ) {
            DisciplinesTab(
                selected = pagerState.currentPage == PAGE_CURRENT,
                label = stringResource(R.string.disciplines_tab_current),
                selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
                unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
                onClick = { scope.launch { pagerState.animateScrollToPage(PAGE_CURRENT) } },
            )
            DisciplinesTab(
                selected = pagerState.currentPage == PAGE_HISTORY,
                label = stringResource(R.string.disciplines_tab_history),
                selectedIcon = Icons.Filled.History,
                unselectedIcon = Icons.Outlined.History,
                onClick = { scope.launch { pagerState.animateScrollToPage(PAGE_HISTORY) } },
            )
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                PAGE_CURRENT -> CurrentPane(
                    semester = current,
                    onOpenDiscipline = onOpenDiscipline,
                    bottomInset = bottomInset,
                )
                PAGE_HISTORY -> HistoryPane(
                    past = past,
                    pending = pending,
                    downloading = downloading,
                    overallScore = overallScore,
                    onOpenDiscipline = onOpenDiscipline,
                    onDownload = onDownload,
                    bottomInset = bottomInset,
                )
            }
        }
    }
}

@Composable
private fun DisciplinesTab(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
) {
    LeadingIconTab(
        selected = selected,
        onClick = onClick,
        selectedContentColor = MaterialTheme.colorScheme.primary,
        unselectedContentColor = MaterialTheme.colorScheme.outline,
        icon = {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        },
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                ),
            )
        },
    )
}

@Composable
private fun Header(semesterCode: String?, isLive: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 10.dp),
    ) {
        Text(
            text = stringResource(R.string.disciplines_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (semesterCode != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatSemesterCode(semesterCode).uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.44.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Dot()
                Text(
                    text = stringResource(
                        if (isLive) R.string.disciplines_semester_ongoing else R.string.disciplines_semester_closed,
                    ).uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.72.sp,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun Dot() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

// ══════════ Atual ══════════

@Composable
private fun CurrentPane(
    semester: Semester?,
    onOpenDiscipline: (Discipline) -> Unit,
    bottomInset: Dp,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val disciplines = semester?.disciplines.orEmpty()
    val flagged = disciplines.filter { it.needsAttention }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = bottomInset + 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (disciplines.isEmpty()) {
            item("current-empty") { EmptyPaneMessage(text = stringResource(R.string.disciplines_current_empty)) }
            return@LazyColumn
        }

        item("current-summary") {
            SemesterSummaryStrip(
                disciplines = disciplines,
                modifier = Modifier.fadeUpOnAppear(delayMs = 120),
            )
        }
        if (flagged.isNotEmpty()) {
            item("current-attention") {
                AttentionBanner(
                    count = flagged.size,
                    detail = attentionDetail(flagged),
                    onClick = {
                        val index = disciplines.indexOfFirst { it.needsAttention }
                        if (index >= 0) {
                            // +2 skips the summary strip and this banner.
                            scope.launch { listState.animateScrollToItem(index + 2) }
                        }
                    },
                    modifier = Modifier.fadeUpOnAppear(delayMs = 160),
                )
            }
        }
        itemsIndexed(
            items = disciplines,
            key = { _, d -> d.fullCode + (d.offerId ?: "") },
        ) { index, discipline ->
            DisciplineCard(
                discipline = discipline,
                onOpen = { onOpenDiscipline(discipline) },
                modifier = Modifier.fadeUpOnAppear(delayMs = 200 + index * 70),
            )
        }
    }
}

@Composable
private fun SemesterSummaryStrip(disciplines: List<Discipline>, modifier: Modifier = Modifier) {
    val averages = disciplines.mapNotNull { it.partialAverage }
    val mean = if (averages.isEmpty()) null else averages.sum() / averages.size

    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(R.plurals.disciplines_count, disciplines.size, disciplines.size),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (mean != null) {
            Dot()
            Text(
                text = stringResource(R.string.disciplines_partial_average_label),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = formatGrade(mean),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private val Discipline.needsAttention: Boolean
    get() = status.key == DisciplineStatus.Key.Final || status.key == DisciplineStatus.Key.Low

@Composable
private fun attentionDetail(flagged: List<Discipline>): String = flagged
    .map {
        when (it.status.key) {
            DisciplineStatus.Key.Final -> stringResource(R.string.disciplines_attention_final_format, it.title)
            else -> stringResource(R.string.disciplines_attention_low_format, it.title)
        }
    }
    .joinToString(" · ")

// ══════════ Histórico ══════════

@Composable
private fun HistoryPane(
    past: List<Semester>,
    pending: List<Semester>,
    downloading: Set<String>,
    overallScore: Double?,
    onOpenDiscipline: (Discipline) -> Unit,
    onDownload: (String) -> Unit,
    bottomInset: Dp,
) {
    // Single-open accordion — the most recent semester starts expanded,
    // matching the dc prototype's `openSem` behavior.
    var openSemester by rememberSaveable(past.firstOrNull()?.id) {
        mutableStateOf(past.firstOrNull()?.id)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = bottomInset + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (past.isEmpty() && pending.isEmpty()) {
            item("history-empty") { EmptyPaneMessage(text = stringResource(R.string.disciplines_history_empty)) }
            return@LazyColumn
        }

        if (past.isNotEmpty()) {
            item("history-summary") {
                val all = past.flatMap { it.disciplines }
                val decided = all.count { it.approved != null }
                val approvalPercent = if (decided > 0) {
                    (all.count { it.approved == true } * 100.0 / decided).roundToInt()
                } else {
                    null
                }
                HistorySummaryCard(
                    overallMean = overallScore,
                    taken = all.size,
                    approvalPercent = approvalPercent,
                    modifier = Modifier
                        .fadeUpOnAppear(delayMs = 120)
                        .padding(bottom = 6.dp),
                )
            }
        }
        itemsIndexed(
            items = past,
            key = { _, sem -> "past-${sem.id}" },
        ) { index, semester ->
            HistorySemesterCard(
                semester = semester,
                open = openSemester == semester.id,
                onToggle = {
                    openSemester = if (openSemester == semester.id) null else semester.id
                },
                onOpenDiscipline = onOpenDiscipline,
                modifier = Modifier.fadeUpOnAppear(delayMs = 180 + index * 60),
            )
        }
        // Pending placeholders — tap kicks off `SyncSemesterUseCase`. KMP
        // re-emits with the semester moved into `past`, so the placeholder
        // disappears once the fetch lands.
        itemsIndexed(
            items = pending,
            key = { _, sem -> "pending-${sem.id}" },
        ) { index, semester ->
            UndownloadedSemesterCard(
                semesterCode = semester.id,
                estimatedCount = semester.estimatedCount,
                isLoading = semester.id in downloading,
                onDownload = { onDownload(semester.id) },
                modifier = Modifier.fadeUpOnAppear(delayMs = 240 + index * 60),
            )
        }
    }
}

@Composable
private fun EmptyPaneMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Preview
@Composable
private fun DisciplinesContentPreview() {
    MelonTheme {
        val palette = MaterialTheme.melon.palette
        DisciplinesContent(
            current = DisciplinesFixtures.CURRENT.tinted(palette),
            isCurrentLive = true,
            past = DisciplinesFixtures.PAST.map { it.tinted(palette) },
            pending = DisciplinesFixtures.PENDING,
            downloading = emptySet(),
            overallScore = DisciplinesFixtures.OVERALL_SCORE,
            onOpenDiscipline = {},
            onDownload = {},
            bottomInset = 0.dp,
        )
    }
}
