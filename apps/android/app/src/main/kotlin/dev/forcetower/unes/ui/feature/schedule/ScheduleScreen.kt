package dev.forcetower.unes.ui.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.MelonPaletteColors
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.overview.ColorFor
import dev.forcetower.unes.ui.feature.schedule.components.ScheduleDayColumn
import dev.forcetower.unes.ui.feature.schedule.components.ScheduleHeader
import dev.forcetower.unes.ui.feature.schedule.components.ScheduleWeekSpine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleClass as KmpScheduleClass
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleDay as KmpScheduleDay
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleWeek as KmpScheduleWeek

// "Horário" tab inside `ConnectedScreen`. Day-focused weekly view: header +
// horizontal week pills + the active day's class column. Mirrors iOS
// `ScheduleFocusedView`. The `ScheduleViewModel` owns the KMP flow + clock
// ticker; this composable maps the raw KMP payload into the local UI
// projection types (the local `ScheduleClass` carries a Color so the column
// stays self-contained — same call iOS made).
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
        onOpenDiscipline = onOpenDiscipline,
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
    onOpenFolioRunner: () -> Unit = {},
) {
    var activeIdx by rememberSaveable { mutableIntStateOf(-1) }
    // Seed the active pill once the first valid emission lands. iOS does the
    // same in `init` via a Calendar lookup; Android waits for KMP because the
    // use case is the source of truth on what counts as "today".
    LaunchedEffect(state.todayIdx >= 0) {
        if (activeIdx < 0 && state.todayIdx >= 0) {
            activeIdx = state.todayIdx.coerceAtLeast(0)
        }
    }

    // `entering` flips false after the first staggered enter pass so subsequent
    // day swaps use the lighter horizontal slide animation on each block.
    var entering by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (entering) {
            delay(1400)
            entering = false
        }
    }

    val surface = MaterialTheme.colorScheme.surface
    val resolvedActive = activeIdx.coerceAtLeast(0)
    val showTodayButton = state.todayIdx != -1 && resolvedActive != state.todayIdx

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface),
    ) {
        AmbientMeshTop(surface = surface, modifier = Modifier.align(Alignment.TopCenter))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset),
        ) {
            ScheduleHeader(
                weekNumber = state.weekNumber,
                weekRange = formatWeekRange(state.firstIso, state.lastIso),
                showTodayButton = showTodayButton,
                onToday = { activeIdx = state.todayIdx.coerceAtLeast(0) },
                entering = entering,
            )

            ScheduleWeekSpine(
                activeIdx = resolvedActive,
                onChange = { activeIdx = it },
                week = week,
                dates = state.dates,
                todayIdx = state.todayIdx,
                entering = entering,
            )

            // Keying on `activeIdx` remounts the day column so its enter
            // animation re-runs on each pill tap — same trick the JSX uses
            // with `<DayColumn key={activeIdx} … />`.
            DayColumnSlot(
                key = resolvedActive,
                classes = week.getOrNull(resolvedActive).orEmpty(),
                isToday = resolvedActive == state.todayIdx,
                nowMin = state.nowMin,
                entering = entering,
                onOpenDiscipline = onOpenDiscipline,
                onOpenFolioRunner = onOpenFolioRunner,
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DayColumnSlot(
    key: Int,
    classes: List<ScheduleClass>,
    isToday: Boolean,
    nowMin: Int,
    entering: Boolean,
    onOpenDiscipline: (ScheduleClass) -> Unit,
    onOpenFolioRunner: () -> Unit,
) {
    key(key) {
        ScheduleDayColumn(
            classes = classes,
            isToday = isToday,
            nowMin = nowMin,
            showGaps = true,
            entering = entering,
            onOpenDiscipline = onOpenDiscipline,
            onOpenFolioRunner = onOpenFolioRunner,
        )
    }
}

@Composable
private fun AmbientMeshTop(surface: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        Mesh(
            variant = MeshVariant.Warm,
            intensity = 0.28f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.95f to surface,
                    ),
                ),
        )
    }
}

// ───────── KMP → UI projection ─────────

private fun mapWeek(
    raw: KmpScheduleWeek?,
    palette: MelonPaletteColors,
): List<List<ScheduleClass>> {
    val days = raw?.days
    if (days.isNullOrEmpty()) return List(7) { emptyList() }
    val bucket = MutableList(7) { emptyList<ScheduleClass>() }
    for (day in days) {
        val idx = day.dayIndex
        if (idx in 0..6) bucket[idx] = mapDay(day, palette)
    }
    return bucket
}

private fun mapDay(day: KmpScheduleDay, palette: MelonPaletteColors): List<ScheduleClass> =
    day.classes.map { mapClass(it, palette) }

private fun mapClass(raw: KmpScheduleClass, palette: MelonPaletteColors): ScheduleClass =
    ScheduleClass(
        start = trimTime(raw.startTime),
        end = raw.endTime?.let(::trimTime).orEmpty(),
        code = raw.code,
        title = raw.title,
        prof = raw.teacherName.orEmpty(),
        color = ColorFor.discipline(palette, raw.code),
        modulo = raw.modulo,
        room = raw.room,
        campus = raw.campus,
        topic = raw.topic,
        offerId = raw.offerId,
    )

// Upstream ships HH:mm or HH:mm:ss — trim to five chars so the time rail
// renders minutes only, matching iOS `ScheduleFocusedViewModel.trimTime`.
private fun trimTime(value: String): String = value.take(5)

// ───────── formatting helpers ─────────

@Composable
private fun formatWeekRange(firstIso: String?, lastIso: String?): String {
    if (firstIso == null || lastIso == null) return ""
    val first = runCatching { LocalDate.parse(firstIso) }.getOrNull() ?: return ""
    val last = runCatching { LocalDate.parse(lastIso) }.getOrNull() ?: return ""
    val firstMonth = formatShortMonth(first)
    val lastMonth = formatShortMonth(last)
    return if (first.monthValue == last.monthValue && first.year == last.year) {
        stringResource(
            R.string.schedule_week_range_same_month_format,
            first.dayOfMonth,
            last.dayOfMonth,
            firstMonth,
        )
    } else {
        stringResource(
            R.string.schedule_week_range_spanning_format,
            first.dayOfMonth,
            firstMonth,
            last.dayOfMonth,
            lastMonth,
        )
    }
}

private val PtBr: Locale = Locale.forLanguageTag("pt-BR")

// Mirrors `OverviewScreen.formatShortDate` post-processing — strip the
// trailing dot some pt-BR locales emit for `MMM` and lowercase. Same shape
// iOS uses in `ScheduleFocusedViewModel.shortMonth`.
private val ShortMonthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM", PtBr)

private fun formatShortMonth(date: LocalDate): String =
    ShortMonthFormatter.format(date)
        .replace(".", "")
        .lowercase(PtBr)

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
