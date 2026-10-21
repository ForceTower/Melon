package dev.forcetower.unes.ui.feature.commute

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.transit.domain.model.CommuteLeg
import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitArrivalState
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitStopBoard
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import kotlin.time.Clock
import kotlin.time.Instant

// The commute line inside the Hoje hero: the invitation before setup, the
// next bus on the way in, the next departure on the way home, and the quiet
// / offline / placeholder states between (dc `MeuTrajetoScreen` · Herói;
// iOS `CommuteHeroLine`). Always drawn on the hero's dark mesh.
@Composable
internal fun CommuteHeroSection(
    vm: CommuteHeroViewModel,
    schedule: CommuteDaySchedule,
    separated: Boolean,
    onOpenBoard: (TransitDirection) -> Unit,
    onOpenSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(schedule) { vm.onIntent(CommuteHeroIntent.ScheduleChanged(schedule)) }
    LifecycleStartEffect(Unit) {
        vm.onIntent(CommuteHeroIntent.Start)
        onStopOrDispose { vm.onIntent(CommuteHeroIntent.Stop) }
    }
    CommuteHeroLine(
        state = state,
        separated = separated,
        onTap = {
            val direction = state.direction
            if (direction == null) {
                vm.trackOpenSetup()
                onOpenSetup()
            } else {
                vm.trackOpenBoard()
                onOpenBoard(direction)
            }
        },
        onSetup = {
            vm.trackOpenSetup()
            onOpenSetup()
        },
        modifier = modifier,
    )
}

@Composable
internal fun CommuteHeroLine(
    state: CommuteHeroUiState,
    separated: Boolean,
    onTap: () -> Unit,
    onSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = rememberCommuteNow()
    val onHero = MaterialTheme.melon.fixed.onHero
    Column(modifier = modifier.fillMaxWidth()) {
        if (separated) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(onHero.copy(alpha = 0.16f)),
            )
            Spacer(Modifier.height(14.dp))
        }
        val leg = state.leg
        val direction = state.direction
        when {
            state.plan.isEmpty -> Invitation(onSetup = onSetup)
            leg == null || direction == null -> Placeholder(
                leg = state.plan.toCampus ?: state.plan.fromCampus,
                failed = state.loadFailed,
            )
            else -> {
                val arrival = state.headline(now)
                val board = state.board
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onTap),
                ) {
                    when {
                        arrival != null && direction == TransitDirection.ToCampus -> Inbound(arrival, leg, state, now)
                        arrival != null -> GoingHome(arrival, leg, state, now)
                        state.isOutdated(now) -> Offline(stopName(leg, direction))
                        board != null -> Quiet(board, stopName(leg, direction))
                        else -> Placeholder(leg = leg, failed = state.loadFailed)
                    }
                }
            }
        }
    }
}

private fun stopName(
    leg: CommuteLeg,
    direction: TransitDirection,
): String = if (direction == TransitDirection.FromCampus) CommuteFormat.departureStopName(leg) else leg.stopName

@Composable
private fun Invitation(onSetup: () -> Unit) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.DirectionsBus,
            contentDescription = null,
            tint = onHero.copy(alpha = 0.8f),
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.commute_hero_invite_title),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = onHero,
            )
            Small(stringResource(R.string.commute_hero_invite_body))
        }
        Text(
            text = stringResource(R.string.commute_hero_invite_action),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.melon.fixed.onSurfaceLight,
            modifier = Modifier
                .clip(CircleShape)
                .background(onHero)
                .clickable(role = Role.Button, onClick = onSetup)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun Inbound(
    arrival: TransitArrival,
    leg: CommuteLeg,
    state: CommuteHeroUiState,
    now: Instant,
) {
    val frozen = state.isOutdated(now)
    val historical = arrival.state == TransitArrivalState.Historical
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TransitLineBadge(number = arrival.lineNumber)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            TransitEstimateText(
                estimate = CommuteFormat.estimate(arrival, now, frozen),
                stale = arrival.signalLost || frozen,
                size = if (arrival.state == TransitArrivalState.Live) 30.sp else 26.sp,
                onDark = true,
            )
            Small(
                stringResource(
                    if (historical) R.string.commute_hero_usually_passes else R.string.commute_hero_at_your_stop,
                    leg.stopName,
                ),
            )
            inboundTail(arrival)?.let { Small(it) }
            StatusRow(arrival = arrival, state = state, now = now)
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.melon.fixed.onHero.copy(alpha = 0.6f),
            modifier = Modifier
                .padding(top = 4.dp)
                .size(20.dp),
        )
    }
}

@Composable
private fun inboundTail(arrival: TransitArrival): String? {
    val seen = arrival.lastSeenStopName
    if (arrival.signalLost && seen != null) return stringResource(R.string.commute_last_seen_near, seen)
    return when (arrival.state) {
        TransitArrivalState.Live -> arrival.campusArrival?.let {
            stringResource(R.string.commute_reaches_campus, clockLabel(it))
        }
        TransitArrivalState.Waiting -> arrival.originDeparture?.let {
            stringResource(R.string.commute_usually_leaves, clockLabel(it))
        }
        TransitArrivalState.Historical -> stringResource(R.string.commute_hero_no_bus_yet)
    }
}

@Composable
private fun StatusRow(
    arrival: TransitArrival,
    state: CommuteHeroUiState,
    now: Instant,
) {
    val offline = state.isOutdated(now)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (offline) {
            TransitChip(
                kind = TransitChipKind.Stale,
                text = stringResource(R.string.commute_state_offline),
                onDark = true,
            )
        } else {
            TransitChip(kind = transitChipKind(arrival), onDark = true)
        }
        if (arrival.isFlaky) TransitChip(kind = TransitChipKind.Flaky, onDark = true)
        Spacer(Modifier.weight(1f))
        val fix = arrival.lastFixAt
        if (!offline && arrival.state != TransitArrivalState.Historical && fix != null) {
            TransitFreshness(fixAt = fix, now = now, signalLost = arrival.signalLost, onDark = true, short = true)
        }
    }
}

@Composable
private fun GoingHome(
    arrival: TransitArrival,
    leg: CommuteLeg,
    state: CommuteHeroUiState,
    now: Instant,
) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TransitLineBadge(number = arrival.lineNumber)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.commute_hero_going_home),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = onHero,
                    )
                    Small(stringResource(R.string.commute_hero_leaves_campus, arrival.lineNumber))
                }
                TransitEstimateText(
                    estimate = CommuteEstimate.Clock(
                        arrival.expected,
                        isApproximate =
                            arrival.state == TransitArrivalState.Historical,
                    ),
                    size = 24.sp,
                    onDark = true,
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = onHero.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.isOutdated(now)) {
                TransitChip(
                    kind = TransitChipKind.Stale,
                    text = stringResource(R.string.commute_state_offline),
                    onDark = true,
                )
            } else {
                DepartureChip(arrival = arrival, now = now, onDark = true)
            }
            Small(CommuteFormat.departureStopName(leg), maxLines = 1, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

@Composable
private fun Quiet(
    board: TransitStopBoard,
    stopName: String,
) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (board.serviceToday) Icons.Outlined.Schedule else Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = onHero.copy(alpha = 0.75f),
            modifier = Modifier.size(20.dp),
        )
        Column {
            Text(
                text = stringResource(
                    if (board.serviceToday) R.string.commute_hero_nothing_soon else R.string.commute_no_service_title,
                ),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = onHero,
            )
            val next = board.nextService
            Small(
                if (next != null) {
                    stringResource(
                        R.string.commute_hero_next_service,
                        next.lineNumber,
                        CommuteFormat.weekdayName(next.usual),
                        clockLabel(next.usual),
                    )
                } else {
                    stopName
                },
            )
        }
    }
}

// Nothing left to show from a board we can no longer refresh: say so rather
// than claim no bus is coming.
@Composable
private fun Offline(stopName: String) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.WifiOff,
            contentDescription = null,
            tint = onHero.copy(alpha = 0.75f),
            modifier = Modifier.size(20.dp),
        )
        Column {
            Text(
                text = stringResource(R.string.commute_offline_title),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = onHero,
            )
            Small(stopName)
        }
    }
}

@Composable
private fun Placeholder(
    leg: CommuteLeg?,
    failed: Boolean,
) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.alpha(0.4f)) {
            TransitLineBadge(number = leg?.lineNumbers?.firstOrNull().orEmpty())
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (failed) {
                Text(
                    text = stringResource(R.string.commute_hero_unavailable),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    color = onHero,
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(onHero.copy(alpha = 0.18f)),
                )
            }
            if (leg != null) Small(stringResource(R.string.commute_hero_at_your_stop, leg.stopName))
        }
    }
}

@Composable
private fun Small(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
        color = MaterialTheme.melon.fixed.onHero.copy(alpha = 0.72f),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

// The commute line on its own dark card, for days the hero has no class.
@Composable
internal fun CommuteHeroCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val veil = MaterialTheme.melon.fixed.heroVeil
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.melon.fixed.heroNight),
    ) {
        Mesh(variant = MeshVariant.Warm, modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to veil.copy(alpha = 0.12f),
                        1f to veil.copy(alpha = 0.6f),
                    ),
                ),
        )
        Box(modifier = Modifier.padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 20.dp)) {
            content()
        }
    }
}

@Preview
@Composable
private fun CommuteHeroPreview() {
    val now = Clock.System.now()
    MelonTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CommuteHeroCard {
                CommuteHeroLine(
                    state = CommuteHeroUiState(
                        enabled = true,
                        plan = CommuteFixtures.plan,
                        direction = TransitDirection.ToCampus,
                        board = CommuteFixtures.board(now),
                        boardReceivedAt = now,
                    ),
                    separated = false,
                    onTap = {},
                    onSetup = {},
                )
            }
            CommuteHeroCard {
                CommuteHeroLine(
                    state = CommuteHeroUiState(
                        enabled = true,
                        plan = CommuteFixtures.plan,
                        direction = TransitDirection.FromCampus,
                        board = CommuteFixtures.board(now, TransitDirection.FromCampus),
                        boardReceivedAt = now,
                    ),
                    separated = false,
                    onTap = {},
                    onSetup = {},
                )
            }
            CommuteHeroCard {
                CommuteHeroLine(state = CommuteHeroUiState(enabled = true), separated = false, onTap = {}, onSetup = {})
            }
        }
    }
}
