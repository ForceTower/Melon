package dev.forcetower.unes.ui.feature.commute

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.transit.domain.model.CommuteLeg
import dev.forcetower.melon.feature.transit.domain.model.CommutePlan
import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitArrivalState
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitFeed
import dev.forcetower.melon.feature.transit.domain.model.TransitFeedStatus
import dev.forcetower.melon.feature.transit.domain.model.TransitStopBoard
import dev.forcetower.melon.feature.transit.domain.model.TransitTodaySlot
import dev.forcetower.melon.feature.transit.domain.model.TransitTodaySlotStatus
import dev.forcetower.melon.feature.transit.domain.model.TransitVehiclePhase
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.MelonBanner
import dev.forcetower.unes.designsystem.components.MelonPrimaryButton
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.settings.components.SettingsSegmentedRow
import kotlin.time.Clock
import kotlin.time.Instant

// The departure the student tapped, handed to the follow screen in-memory so
// it opens with the board's number.
internal data class CommuteFollowSeed(
    val arrival: TransitArrival,
    val direction: TransitDirection,
    val stopId: String,
    val receivedAt: Instant,
)

// "Meu trajeto": the saved stop's next buses ("Meu ponto" on the way in,
// "Saindo da UEFS" on the way out), the first-use intro when nothing is saved
// (dc `MeuTrajetoScreen` · Meu ponto / Saída / Primeiro uso; iOS
// `CommuteBoardView`).
@Composable
internal fun CommuteBoardScreen(
    direction: TransitDirection?,
    onBack: () -> Unit,
    onFollow: (CommuteFollowSeed) -> Unit,
    onEditPlan: (CommutePlan) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CommuteBoardViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(direction) { vm.onIntent(CommuteBoardIntent.Open(direction)) }
    LifecycleStartEffect(Unit) {
        vm.onIntent(CommuteBoardIntent.Start)
        onStopOrDispose { vm.onIntent(CommuteBoardIntent.Stop) }
    }

    CommuteBoardContent(
        state = state,
        onBack = onBack,
        onIntent = vm::onIntent,
        onFollow = { arrival ->
            val leg = state.leg ?: return@CommuteBoardContent
            val current = state.direction ?: return@CommuteBoardContent
            vm.trackFollowTap()
            onFollow(CommuteFollowSeed(arrival, current, leg.stopId, state.boardReceivedAt ?: Clock.System.now()))
        },
        onEditPlan = {
            vm.trackSetupTap()
            onEditPlan(state.plan)
        },
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommuteBoardContent(
    state: CommuteBoardUiState,
    onBack: () -> Unit,
    onIntent: (CommuteBoardIntent) -> Unit,
    onFollow: (TransitArrival) -> Unit,
    onEditPlan: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val background = MaterialTheme.colorScheme.background
    val now = rememberCommuteNow()
    val leg = state.leg
    val direction = state.direction

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        TopAppBar(
            title = { Text(text = stringResource(R.string.commute_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.commute_back),
                    )
                }
            },
            actions = {
                if (leg != null) {
                    IconButton(onClick = onEditPlan) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.commute_settings),
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = background,
                scrolledContainerColor = background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 24.dp + bottomInset),
        ) {
            if (state.plan.directions.size > 1 && direction != null) {
                SettingsSegmentedRow(
                    options = TransitDirection.entries,
                    selected = direction,
                    optionLabel = { stringResource(it.labelRes) },
                    onSelect = { onIntent(CommuteBoardIntent.DirectionPicked(it)) },
                    modifier = Modifier.padding(bottom = 18.dp),
                )
            }
            when {
                leg != null && direction != null -> {
                    BoardHeader(leg = leg, direction = direction)
                    BoardBody(
                        state = state,
                        leg = leg,
                        direction = direction,
                        now = now,
                        onIntent = onIntent,
                        onFollow = onFollow,
                    )
                }
                state.planLoaded -> CommuteIntro(onStart = onEditPlan)
                else -> Loading()
            }
        }
    }

    if (state.explainerOpen) {
        CommuteHowSheet(arrival = state.explained, onDismiss = { onIntent(CommuteBoardIntent.ExplainerDismissed) })
    }
}

internal val TransitDirection.labelRes: Int
    get() = when (this) {
        TransitDirection.ToCampus -> R.string.commute_direction_to_campus
        TransitDirection.FromCampus -> R.string.commute_direction_from_campus
    }

@Composable
private fun BoardHeader(
    leg: CommuteLeg,
    direction: TransitDirection,
) {
    val toCampus = direction == TransitDirection.ToCampus
    Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = if (toCampus) Icons.Filled.LocationOn else Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = stringResource(
                    if (toCampus) R.string.commute_board_eyebrow_to_campus else R.string.commute_direction_from_campus,
                ),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Text(
            text = if (toCampus) leg.stopName else CommuteFormat.departureStopName(leg),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = if (toCampus) {
                listOfNotNull(
                    leg.stopArea,
                    stringResource(R.string.commute_walk_minutes, leg.walkMinutes),
                    stringResource(R.string.commute_lines_list, lineListLabel(leg.lineNumbers)),
                ).joinToString(" · ")
            } else {
                stringResource(R.string.commute_departure_stop_subtitle)
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun BoardBody(
    state: CommuteBoardUiState,
    leg: CommuteLeg,
    direction: TransitDirection,
    now: Instant,
    onIntent: (CommuteBoardIntent) -> Unit,
    onFollow: (TransitArrival) -> Unit,
) {
    val board = state.board
    when {
        board != null -> {
            val receivedAt = state.boardReceivedAt
            if ((state.loadFailed || state.isOutdated(now)) && receivedAt != null) {
                OfflineNotice(receivedAt)
                Spacer(Modifier.height(14.dp))
            } else if (board.feed.status != TransitFeedStatus.Ok) {
                FeedNotice(board.feed)
                Spacer(Modifier.height(14.dp))
            }
            if (!board.serviceToday) {
                CommuteNoServiceCard(board = board, leg = leg)
            } else {
                ArrivalsSection(
                    state = state,
                    board = board,
                    direction = direction,
                    now = now,
                    onIntent = onIntent,
                    onFollow = onFollow,
                )
                if (direction == TransitDirection.ToCampus) {
                    TodaySection(board = board)
                } else {
                    board.arrivals.firstOrNull { it.isFlaky }?.let { FlakyNotice(it) }
                }
            }
            if (direction == TransitDirection.ToCampus) {
                Text(
                    text = stringResource(R.string.commute_board_footnote),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
        state.loadFailed -> MelonBanner(
            title = stringResource(R.string.commute_unavailable_title),
            detail = stringResource(R.string.commute_unavailable_body_no_data),
            tone = MaterialTheme.colorScheme.outline,
            icon = Icons.Filled.CloudOff,
        )
        else -> Loading()
    }
}

@Composable
private fun Loading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun OfflineNotice(receivedAt: Instant) {
    MelonBanner(
        title = stringResource(R.string.commute_offline_title),
        detail = stringResource(R.string.commute_offline_body, clockLabel(receivedAt)),
        tone = MaterialTheme.colorScheme.outline,
        icon = Icons.Filled.WifiOff,
    )
}

@Composable
private fun FeedNotice(feed: TransitFeed) {
    val last = feed.lastUpdateAt
    MelonBanner(
        title = stringResource(R.string.commute_unavailable_title),
        detail = if (last != null) {
            stringResource(R.string.commute_unavailable_body, clockLabel(last))
        } else {
            stringResource(R.string.commute_unavailable_body_no_data)
        },
        tone = MaterialTheme.colorScheme.outline,
        icon = Icons.Filled.CloudOff,
    )
}

@Composable
private fun FlakyNotice(arrival: TransitArrival) {
    val time = arrival.scheduledDeparture ?: clockLabel(arrival.expected)
    val evening = CommuteFormat.local(arrival.expected).hour >= 18
    MelonBanner(
        title = stringResource(R.string.commute_state_flaky),
        detail = stringResource(
            if (evening) R.string.commute_flaky_body_nights else R.string.commute_flaky_body,
            arrival.lineNumber,
            time,
            arrival.ranDays ?: 0,
            arrival.observedDays ?: 0,
        ),
        tone = MaterialTheme.colorScheme.outline,
        icon = Icons.AutoMirrored.Outlined.HelpOutline,
        modifier = Modifier.padding(bottom = 18.dp),
    )
}

@Composable
private fun ArrivalsSection(
    state: CommuteBoardUiState,
    board: TransitStopBoard,
    direction: TransitDirection,
    now: Instant,
    onIntent: (CommuteBoardIntent) -> Unit,
    onFollow: (TransitArrival) -> Unit,
) {
    val toCampus = direction == TransitDirection.ToCampus
    val frozen = state.isOutdated(now)
    val degraded = board.feed.status != TransitFeedStatus.Ok || frozen
    // Without fresh data, a bus whose window already closed can't be vouched for.
    val arrivals = if (frozen) board.arrivals.filter { it.latest >= now } else board.arrivals
    val lastInfo = if (board.feed.status != TransitFeedStatus.Ok) board.feed.lastUpdateAt else state.boardReceivedAt

    Column(modifier = Modifier.padding(bottom = 18.dp)) {
        CommuteSectionHeader(
            title = stringResource(
                if (toCampus) R.string.commute_next_title else R.string.commute_next_departures_title,
            ),
            hint = if (degraded && lastInfo != null) {
                stringResource(R.string.commute_last_info_at, clockLabel(lastInfo))
            } else {
                stringResource(if (toCampus) R.string.commute_next_hint else R.string.commute_next_departures_hint)
            },
            action = stringResource(R.string.commute_how_title),
            onAction = { onIntent(CommuteBoardIntent.ExplainTapped(null)) },
        )
        if (arrivals.isEmpty()) {
            Text(
                text = stringResource(R.string.commute_nothing_soon),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .commuteCard()
                    .alpha(if (degraded) 0.6f else 1f),
            ) {
                arrivals.forEachIndexed { index, arrival ->
                    CommuteArrivalRow(
                        arrival = arrival,
                        direction = direction,
                        now = now,
                        frozen = frozen,
                        onClick = { onFollow(arrival) },
                    )
                    if (index < arrivals.lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.melon.surface.line)
                    }
                }
            }
        }
    }
}

// One departure: the badge, the band, the detail line and the chips that say
// what the band rests on. Tapping follows that bus.
@Composable
internal fun CommuteArrivalRow(
    arrival: TransitArrival,
    direction: TransitDirection,
    now: Instant,
    frozen: Boolean,
    onClick: () -> Unit,
) {
    val toCampus = direction == TransitDirection.ToCampus
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = 16.dp,
                end = 14.dp,
                top = if (toCampus) 14.dp else 13.dp,
                bottom = if (toCampus) 14.dp else 13.dp,
            ),
        verticalAlignment = if (toCampus) Alignment.Top else Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TransitLineBadge(
            number = arrival.lineNumber,
            modifier = Modifier.padding(top = if (toCampus) 3.dp else 0.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            if (toCampus) InboundRow(arrival, now, frozen) else DepartureRow(arrival, now, frozen)
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .padding(top = if (toCampus) 8.dp else 0.dp)
                .size(20.dp),
        )
    }
}

@Composable
private fun InboundRow(
    arrival: TransitArrival,
    now: Instant,
    frozen: Boolean,
) {
    if (arrival.missing) {
        Text(
            text = stringResource(
                R.string.commute_missing_title,
                arrival.lineNumber,
                arrival.scheduledDeparture ?: clockLabel(arrival.expected),
            ),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.5.sp),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.commute_missing_body, clockLabel(arrival.expected)),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp),
        )
    } else {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TransitEstimateText(
                estimate = CommuteFormat.estimate(arrival, now, frozen),
                dim = arrival.state == TransitArrivalState.Historical || arrival.signalLost || frozen,
                stale = arrival.signalLost || frozen,
                size = if (arrival.state == TransitArrivalState.Live) 30.sp else 26.sp,
            )
            val stops = arrival.stopsAway
            if (arrival.state == TransitArrivalState.Live && !arrival.signalLost && !frozen && stops != null &&
                stops > 0
            ) {
                Text(
                    text = stopsAwayLabel(stops),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        val detail = inboundDetail(arrival)
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = if (arrival.state == TransitArrivalState.Historical) {
                    MaterialTheme.colorScheme.outlineVariant
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
    Row(
        modifier = Modifier.padding(top = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (frozen) {
            TransitChip(
                kind = TransitChipKind.Stale,
                text = stringResource(R.string.commute_state_offline),
                small = true,
            )
        } else {
            TransitChip(
                kind = if (arrival.missing) TransitChipKind.Historical else transitChipKind(arrival),
                small = true,
            )
        }
        if (arrival.isFlaky) TransitChip(kind = TransitChipKind.Flaky, small = true)
        val fix = arrival.lastFixAt
        if (!frozen && arrival.state != TransitArrivalState.Historical && fix != null) {
            TransitFreshness(fixAt = fix, now = now, signalLost = arrival.signalLost)
        }
    }
}

@Composable
private fun inboundDetail(arrival: TransitArrival): String? {
    val seen = arrival.lastSeenStopName
    val fix = arrival.lastFixAt
    if (arrival.signalLost && seen != null && fix != null) {
        return stringResource(R.string.commute_last_seen_near_at, seen, clockLabel(fix))
    }
    return when (arrival.state) {
        TransitArrivalState.Live -> arrival.campusArrival?.let {
            stringResource(R.string.commute_reaches_campus, clockLabel(it))
        }
        TransitArrivalState.Waiting -> arrival.originDeparture?.let {
            stringResource(R.string.commute_usually_leaves_terminal, clockLabel(it))
        }
        TransitArrivalState.Historical -> stringResource(
            R.string.commute_usually_passes_around,
            clockLabel(arrival.expected),
        )
    }
}

@Composable
private fun DepartureRow(
    arrival: TransitArrival,
    now: Instant,
    frozen: Boolean,
) {
    TransitEstimateText(
        estimate = CommuteEstimate.Clock(arrival.expected, isApproximate = false),
        dim = arrival.state == TransitArrivalState.Historical || frozen,
        size = 24.sp,
    )
    Row(
        modifier = Modifier.padding(top = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (frozen) {
            TransitChip(
                kind = TransitChipKind.Stale,
                text = stringResource(R.string.commute_state_offline),
                small = true,
            )
        } else {
            DepartureChip(arrival = arrival, now = now, small = true)
        }
        val fix = arrival.lastFixAt
        if (!frozen && arrival.state != TransitArrivalState.Historical && fix != null) {
            TransitFreshness(fixAt = fix, now = now, signalLost = arrival.signalLost, short = true)
        }
    }
}

// What a bus leaving campus is doing right now: already at the stop, still
// coming from the Terminal, or only its usual time.
@Composable
internal fun DepartureChip(
    arrival: TransitArrival,
    now: Instant,
    small: Boolean = false,
    onDark: Boolean = false,
) {
    when (arrival.phase) {
        TransitVehiclePhase.AtStop, TransitVehiclePhase.AtOrigin -> TransitChip(
            kind = TransitChipKind.Live,
            text = stringResource(if (onDark) R.string.commute_bus_already_at_stop else R.string.commute_bus_at_stop),
            small = small,
            onDark = onDark,
        )
        TransitVehiclePhase.Inbound -> {
            val originArrival = arrival.originArrival
            if (originArrival != null) {
                TransitChip(
                    kind = TransitChipKind.Live,
                    text = stringResource(
                        if (onDark) {
                            R.string.commute_arriving_at_campus_in
                        } else {
                            R.string.commute_arriving_from_terminal_in
                        },
                        CommuteFormat.minutesUntil(originArrival, now),
                    ),
                    small = small,
                    onDark = onDark,
                )
            } else {
                TransitChip(kind = transitChipKind(arrival), small = small, onDark = onDark)
            }
        }
        TransitVehiclePhase.EnRoute, null -> TransitChip(
            kind = if (arrival.isFlaky) TransitChipKind.Flaky else transitChipKind(arrival),
            small = small,
            onDark = onDark,
        )
    }
}

// ───────── Hoje ─────────

@Composable
private fun TodaySection(board: TransitStopBoard) {
    Column(modifier = Modifier.padding(bottom = 18.dp)) {
        CommuteSectionHeader(
            title = stringResource(R.string.commute_today_title),
            hint = stringResource(R.string.commute_today_hint),
        )
        if (board.today.isEmpty()) {
            Text(
                text = stringResource(R.string.commute_today_empty),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        } else {
            CommuteTodayStrip(slots = todayWindow(board.today))
            Text(
                text = stringResource(R.string.commute_today_footnote),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp),
            )
        }
    }
}

// The server lists the whole day; a couple of passed slots give the next
// one its context.
internal fun todayWindow(slots: List<TransitTodaySlot>): List<TransitTodaySlot> {
    val pivot = slots.indexOfFirst { it.status != TransitTodaySlotStatus.Passed }.takeIf { it >= 0 } ?: slots.size
    return slots.subList((pivot - 2).coerceAtLeast(0), (pivot + 8).coerceAtMost(slots.size))
}

@Composable
private fun CommuteTodayStrip(slots: List<TransitTodaySlot>) {
    val listState = rememberLazyListState()
    LaunchedEffect(slots) {
        val next = slots.indexOfFirst { it.status == TransitTodaySlotStatus.Next }
        if (next > 0) listState.scrollToItem(next)
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        // Bleeds past the column's gutters so the tiles scroll edge to edge.
        modifier = Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val bleed = 16.dp.roundToPx()
                val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + bleed * 2))
                layout(placeable.width - bleed * 2, placeable.height) { placeable.place(-bleed, 0) }
            },
    ) {
        items(slots, key = { it.id }) { slot -> TodayTile(slot) }
    }
}

@Composable
private fun TodayTile(slot: TransitTodaySlot) {
    val isNext = slot.status == TransitTodaySlotStatus.Next
    val passed = slot.status == TransitTodaySlotStatus.Passed
    val ink = MaterialTheme.colorScheme.onBackground
    val onInk = MaterialTheme.colorScheme.background
    val shape = RoundedCornerShape(14.dp)
    val borderColor = when {
        slot.isFlaky -> MaterialTheme.colorScheme.outlineVariant
        isNext -> ink
        else -> MaterialTheme.melon.surface.line
    }
    Column(
        modifier = Modifier
            .alpha(if (passed) 0.45f else 1f)
            .widthIn(min = 74.dp)
            .clip(shape)
            .background(if (isNext) ink else MaterialTheme.melon.surface.card)
            .border(1.dp, borderColor, shape)
            .padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = slot.lineNumber,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 0.sp),
                color = if (isNext) onInk else MaterialTheme.melon.transit.line(slot.lineNumber),
            )
            if (slot.isFlaky) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = stringResource(R.string.commute_state_flaky),
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        Text(
            text = clockLabel(slot.usual),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, letterSpacing = (-0.57).sp),
            color = if (isNext) onInk else ink,
            textDecoration = if (passed) TextDecoration.LineThrough else null,
            modifier = Modifier.padding(top = 4.dp),
        )
        val scheduled = slot.scheduledDeparture
        if (scheduled != null) {
            Text(
                text = stringResource(R.string.commute_today_official, scheduled),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = if (isNext) onInk.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// ───────── Edge states ─────────

@Composable
private fun CommuteNoServiceCard(
    board: TransitStopBoard,
    leg: CommuteLeg,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .commuteCard()
            .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.EventBusy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.commute_no_service_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.commute_no_service_body, lineListLabel(leg.lineNumbers)),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        val next = board.nextService
        if (next != null) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp, bottom = 14.dp),
                thickness = 1.dp,
                color = MaterialTheme.melon.surface.line,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.commute_next_trip, CommuteFormat.weekdayName(next.usual)),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f),
                )
                TransitLineBadge(number = next.lineNumber, size = TransitBadgeSize.Small)
                Spacer(Modifier.size(8.dp))
                TransitEstimateText(
                    estimate = CommuteEstimate.Clock(next.usual, isApproximate = true),
                    dim = true,
                    size = 22.sp,
                )
            }
        }
    }
    Spacer(Modifier.height(18.dp))
}

// First use: what the screen will show, and the way into setup.
@Composable
internal fun CommuteIntro(onStart: () -> Unit) {
    Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.DirectionsBus,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = stringResource(R.string.commute_intro_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            text = stringResource(R.string.commute_intro_body),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp, lineHeight = 23.sp),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 10.dp),
        )
        Column(
            modifier = Modifier.padding(top = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IntroBullet(Icons.Filled.Sensors, R.string.commute_state_live, R.string.commute_intro_live)
            IntroBullet(Icons.Filled.LocalParking, R.string.commute_state_waiting, R.string.commute_intro_waiting)
            IntroBullet(Icons.Filled.History, R.string.commute_state_historical, R.string.commute_intro_historical)
        }
        MelonPrimaryButton(
            text = stringResource(R.string.commute_intro_action),
            onClick = onStart,
            modifier = Modifier.padding(top = 32.dp),
        )
        Text(
            text = stringResource(R.string.commute_works_without_location),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun IntroBullet(
    icon: ImageVector,
    titleRes: Int,
    bodyRes: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CommuteBulletIcon(icon = icon)
        Column {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.5.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Preview
@Composable
private fun CommuteBoardPreview() {
    val now = Clock.System.now()
    MelonTheme {
        CommuteBoardContent(
            state = CommuteBoardUiState(
                plan = CommuteFixtures.plan,
                planLoaded = true,
                direction = TransitDirection.ToCampus,
                board = CommuteFixtures.board(now),
                boardReceivedAt = now,
                network = CommuteFixtures.network,
            ),
            onBack = {},
            onIntent = {},
            onFollow = {},
            onEditPlan = {},
        )
    }
}

@Preview
@Composable
private fun CommuteIntroPreview() {
    MelonTheme {
        CommuteBoardContent(
            state = CommuteBoardUiState(planLoaded = true),
            onBack = {},
            onIntent = {},
            onFollow = {},
            onEditPlan = {},
        )
    }
}
