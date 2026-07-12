package dev.forcetower.unes.ui.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeInOnAppear
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.calendar.components.CalCategorySegmented
import dev.forcetower.unes.ui.feature.calendar.components.CalEventRow
import dev.forcetower.unes.ui.feature.calendar.components.CalEventSheet
import dev.forcetower.unes.ui.feature.calendar.components.CalHeroCard
import dev.forcetower.unes.ui.feature.calendar.components.CalMonthGrid
import dev.forcetower.unes.ui.feature.calendar.components.CalScopeChips
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

// Academic-calendar screen — every UEFS-side date the student should know
// about (deadlines, exams, holidays), with an Agenda / Grade toggle on the
// app bar, a "próxima ação" mesh hero, category + scope filters, and an M3
// bottom sheet per event. Pushed from the "Calendário" shortcut on the Me
// hub. Mirrors the dc `UNES Calendário - Android`.
@Composable
internal fun CalendarScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CalendarViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    CalendarContent(
        events = state.events,
        onBack = onBack,
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@Composable
private fun CalendarContent(
    events: List<CalendarEvent>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    var category by rememberSaveable { mutableStateOf(CalendarCategoryFilter.All) }
    var scope by rememberSaveable { mutableStateOf(CalendarScopeFilter.All) }
    var viewMode by rememberSaveable { mutableStateOf(CalendarViewMode.Agenda) }
    var openEventId by rememberSaveable { mutableStateOf<String?>(null) }

    val today = remember { CalendarMath.today }
    // YearMonth isn't Saveable — persist the grid cursor as a proleptic-month
    // count and the selection as an epoch day.
    var gridMonthIndex by rememberSaveable {
        mutableLongStateOf(today.year * 12L + today.monthValue - 1)
    }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val gridMonth = YearMonth.of((gridMonthIndex / 12).toInt(), (gridMonthIndex % 12).toInt() + 1)
    val selected = LocalDate.ofEpochDay(selectedEpochDay)

    val filtered = remember(events, category, scope) {
        events.filter { category.matches(it) && scope.matches(it) }
    }
    // Hide past events from the agenda, but keep events that are still active
    // even though they started in the past — so the hero + agenda agree.
    val visible = remember(filtered) {
        filtered.filter { CalendarMath.status(it) != CalendarStatus.Past }
    }
    val monthGroups = remember(visible) { visible.groupedByMonth() }
    val hero = remember(filtered) { CalendarMath.nextDeadline(filtered) }
    // Resolved against the unfiltered list so the sheet survives filter swaps.
    val openEvent = remember(events, openEventId) { events.find { it.id == openEventId } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomInset + 32.dp),
    ) {
        AppBar(
            viewMode = viewMode,
            onBack = onBack,
            onViewModeChange = { viewMode = it },
            modifier = Modifier.fadeInOnAppear(delayMs = 20),
        )
        Headline(modifier = Modifier.fadeInOnAppear(delayMs = 40))

        if (hero != null) {
            CalHeroCard(
                event = hero,
                onClick = { openEventId = hero.id },
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp)
                    .fadeUpOnAppear(delayMs = 100, fromOffset = 20.dp),
            )
        }

        CalCategorySegmented(
            active = category,
            onChange = { category = it },
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 18.dp)
                .fadeUpOnAppear(delayMs = 200),
        )
        CalScopeChips(
            active = scope,
            onChange = { scope = it },
            modifier = Modifier
                .padding(top = 12.dp)
                .fadeUpOnAppear(delayMs = 260),
        )

        when (viewMode) {
            CalendarViewMode.Agenda -> AgendaBody(monthGroups = monthGroups, onOpen = { openEventId = it })
            CalendarViewMode.Grid -> GridBody(
                month = gridMonth,
                events = filtered,
                selected = selected,
                today = today,
                onSelect = { selectedEpochDay = it.toEpochDay() },
                onMonthShift = { gridMonthIndex += it },
                onOpen = { openEventId = it },
            )
        }

        SyncFooter(modifier = Modifier.fadeUpOnAppear(delayMs = 440))
    }

    if (openEvent != null) {
        CalEventSheet(event = openEvent, onDismiss = { openEventId = null })
    }
}

@Composable
private fun AppBar(
    viewMode: CalendarViewMode,
    onBack: () -> Unit,
    onViewModeChange: (CalendarViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.calendar_back_label),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        ViewToggleButton(
            active = viewMode == CalendarViewMode.Agenda,
            activeIcon = Icons.Filled.ViewAgenda,
            inactiveIcon = Icons.Outlined.ViewAgenda,
            label = stringResource(R.string.calendar_view_agenda_label),
            onClick = { onViewModeChange(CalendarViewMode.Agenda) },
        )
        ViewToggleButton(
            active = viewMode == CalendarViewMode.Grid,
            activeIcon = Icons.Filled.CalendarMonth,
            inactiveIcon = Icons.Outlined.CalendarMonth,
            label = stringResource(R.string.calendar_view_grid_label),
            onClick = { onViewModeChange(CalendarViewMode.Grid) },
        )
    }
}

@Composable
private fun ViewToggleButton(
    active: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            } else {
                Color.Transparent
            },
            contentColor = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
    ) {
        Icon(
            imageVector = if (active) activeIcon else inactiveIcon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun Headline(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        Text(
            text = stringResource(R.string.calendar_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.64).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.calendar_header_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}

@Composable
private fun AgendaBody(
    monthGroups: List<CalendarMonthGroup>,
    onOpen: (String) -> Unit,
) {
    if (monthGroups.isEmpty()) {
        EmptyState()
        return
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fadeUpOnAppear(delayMs = 340),
    ) {
        monthGroups.forEach { group ->
            key(group.id) {
                MonthSection(group = group, onOpen = onOpen)
            }
        }
    }
}

@Composable
private fun MonthSection(group: CalendarMonthGroup, onOpen: (String) -> Unit) {
    val today = remember { CalendarMath.today }
    val isCurrent = today.year == group.year && today.monthValue == group.month
    val monthName = CalendarFormat.monthsLong[group.month - 1]
        .replaceFirstChar { it.titlecase(Locale.ROOT) }

    Column(modifier = Modifier.padding(top = 10.dp)) {
        Row(
            modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                text = monthName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.44).sp,
                ),
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.calendar_month_summary,
                    group.events.size,
                    group.year,
                    group.events.size,
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(bottom = 1.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            group.events.forEach { event ->
                key(event.id) {
                    CalEventRow(event = event, onClick = { onOpen(event.id) })
                }
            }
        }
    }
}

@Composable
private fun GridBody(
    month: YearMonth,
    events: List<CalendarEvent>,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onMonthShift: (Long) -> Unit,
    onOpen: (String) -> Unit,
) {
    val selectedEvents = remember(events, selected) {
        events.filter { CalendarMath.occursOn(it, selected) }.sortedBy { it.start }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fadeUpOnAppear(delayMs = 340),
    ) {
        CalMonthGrid(
            month = month,
            events = events,
            selected = selected,
            onSelect = onSelect,
            onPrevMonth = { onMonthShift(-1L) },
            onNextMonth = { onMonthShift(1L) },
            modifier = Modifier.padding(top = 12.dp),
        )

        Row(
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (selected == today) {
                    stringResource(R.string.calendar_grid_selected_today)
                } else {
                    stringResource(
                        R.string.calendar_grid_selected_day_format,
                        selected.dayOfMonth,
                        CalendarFormat.monthsLong[selected.monthValue - 1],
                    )
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    letterSpacing = (-0.36).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = CalendarFormat.weekday(selected),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(bottom = 1.dp),
            )
        }

        if (selectedEvents.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.melon.surface.card,
                border = BorderStroke(1.dp, MaterialTheme.melon.surface.line),
            ) {
                Text(
                    text = stringResource(R.string.calendar_grid_empty),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                selectedEvents.forEach { event ->
                    key(event.id) {
                        CalEventRow(
                            event = event,
                            onClick = { onOpen(event.id) },
                            showDate = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncFooter(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.calendar_sync_footer),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 22.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun EmptyState() {
    Text(
        text = stringResource(R.string.calendar_empty_state),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 80.dp),
        textAlign = TextAlign.Center,
    )
}

@Preview
@Composable
private fun CalendarPreview() {
    MelonTheme {
        CalendarContent(events = CalendarFixtures.events, onBack = {})
    }
}
