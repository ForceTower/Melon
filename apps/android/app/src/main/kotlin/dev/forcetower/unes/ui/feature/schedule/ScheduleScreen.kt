package dev.forcetower.unes.ui.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.PinnedHeaderHairline
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.schedule.components.ScheduleDayHeader
import dev.forcetower.unes.ui.feature.schedule.components.ScheduleEmptyDay
import dev.forcetower.unes.ui.feature.schedule.components.ScheduleHeader
import dev.forcetower.unes.ui.feature.schedule.components.ScheduleTimeline
import dev.forcetower.unes.ui.feature.schedule.components.ScheduleWeekRail

// "Horário" tab inside `ConnectedScreen` — 2026 redesign (dc project `UNES
// Horário - Android`): M3 large-style app bar with the week eyebrow, the
// tonal week date rail, the selected-day header with the meta chip, and a
// connected timeline of tonal per-discipline cards with expandable quick
// actions. A "Hoje" FAB jumps back to today whenever another day is
// selected. Free days keep the Folio mascot (long-press opens the runner
// easter egg). The `ScheduleViewModel` owns the KMP flow; this composable
// maps the raw KMP payload into the local UI projection types.
@Composable
internal fun ScheduleScreen(
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    onOpenDiscipline: (ScheduleClass) -> Unit = {},
    onOpenFolioRunner: () -> Unit = {},
) {
    val vm: ScheduleViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val palette = MaterialTheme.melon.palette
    val week = remember(state.raw, palette) { mapWeek(state.raw, palette) }

    ScheduleContent(
        state = state,
        week = week,
        onOpenDiscipline = { cls -> vm.trackOpenDiscipline(cls); onOpenDiscipline(cls) },
        onDaySelect = vm::trackDaySelect,
        onOpenFolioRunner = onOpenFolioRunner,
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@Composable
private fun ScheduleContent(
    state: ScheduleUiState,
    week: List<List<ScheduleClass>>,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    onOpenDiscipline: (ScheduleClass) -> Unit = {},
    onDaySelect: (String?, Int) -> Unit = { _, _ -> },
    onOpenFolioRunner: () -> Unit = {},
) {
    var activeIdx by rememberSaveable { mutableIntStateOf(-1) }
    // Seed the active day once the first valid emission lands — the KMP use
    // case is the source of truth on what counts as "today".
    LaunchedEffect(state.todayIdx >= 0) {
        if (activeIdx < 0 && state.todayIdx >= 0) {
            activeIdx = state.todayIdx.coerceAtLeast(0)
        }
    }
    // Expanded quick-actions card, keyed "<code>-<index>"; empty = none.
    // Collapses when another day is picked, same as the dc prototype.
    var expandedId by rememberSaveable { mutableStateOf("") }

    val resolvedActive = activeIdx.coerceAtLeast(0)
    val classes = week.getOrNull(resolvedActive).orEmpty()
    val activeIso = state.dateIsos.getOrNull(resolvedActive)

    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 0 } }

    // Header + week rail stay pinned (matching iOS); only the day header and
    // the timeline scroll beneath them.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        ScheduleHeader(
            weekNumber = state.weekNumber,
            weekRange = formatWeekRange(state.firstIso, state.lastIso),
            modifier = Modifier.fadeUpOnAppear(
                delayMs = 80,
                durationMs = 500,
                fromOffset = (-10).dp,
            ),
        )
        ScheduleWeekRail(
            activeIdx = resolvedActive,
            todayIdx = state.todayIdx,
            dates = state.dates,
            counts = week.map { it.size },
            onSelect = {
                activeIdx = it
                expandedId = ""
                onDaySelect(state.dateIsos.getOrNull(it), it)
            },
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .padding(top = 14.dp)
                .fadeUpOnAppear(delayMs = 140, durationMs = 550, fromOffset = (-8).dp),
        )
        PinnedHeaderHairline(scrolled = scrolled)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = bottomInset + 32.dp),
        ) {
            ScheduleDayHeader(
                dayName = remember(activeIso) { formatDayName(activeIso) },
                dayDate = formatDayDate(activeIso),
                meta = dayMeta(classes),
            )
            // Keying on the active day remounts the timeline so the row enter
            // animation re-runs on each rail tap — same trick the dc
            // prototype uses by re-rendering the `sc-for`.
            key(resolvedActive) {
                if (classes.isEmpty()) {
                    ScheduleEmptyDay(onLongPress = onOpenFolioRunner)
                } else {
                    ScheduleTimeline(
                        classes = classes,
                        expandedId = expandedId.ifEmpty { null },
                        onToggle = { id -> expandedId = if (expandedId == id) "" else id },
                        onOpenDiscipline = onOpenDiscipline,
                    )
                }
            }
        }
    }
}

// ───────── formatting helpers ─────────

@Composable
private fun dayMeta(classes: List<ScheduleClass>): String {
    if (classes.isEmpty()) return stringResource(R.string.schedule_day_meta_empty)
    return stringResource(
        R.string.schedule_day_meta_format,
        pluralStringResource(R.plurals.schedule_day_meta_classes, classes.size, classes.size),
        classes.first().start,
        classes.last().end,
    )
}

@Preview
@Composable
private fun ScheduleScreenPreview() {
    MelonTheme {
        val palette = MaterialTheme.melon.palette
        val raw = ScheduleFixtures.kmpWeek()
        val week = mapWeek(raw, palette)
        ScheduleContent(
            state = ScheduleUiState(raw = raw),
            week = week,
        )
    }
}
