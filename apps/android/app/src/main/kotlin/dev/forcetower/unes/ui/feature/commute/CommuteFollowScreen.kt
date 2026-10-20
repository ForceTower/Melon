package dev.forcetower.unes.ui.feature.commute

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitArrivalState
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitRoute
import dev.forcetower.melon.feature.transit.domain.model.TransitStop
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.MelonBanner
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.connected.ConnectedRoute
import kotlin.time.Clock
import kotlin.time.Instant

// "Seguindo o 087": the band to the student's stop, the schematic map with
// only this line's buses, and the route line (dc `MeuTrajetoScreen` ·
// Seguindo; iOS `CommuteFollowView`). Showing the student's own location is
// a FAB on the map, never a default.
@Composable
internal fun CommuteFollowScreen(
    route: ConnectedRoute.CommuteFollow,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CommuteFollowViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(route) {
        vm.onIntent(
            CommuteFollowIntent.Open(
                lineNumber = route.lineNumber,
                direction = TransitDirection.fromWire(route.direction) ?: TransitDirection.ToCampus,
                stopId = route.stopId,
                arrivalId = route.arrivalId,
                vehicleId = route.vehicleId,
            ),
        )
    }
    LifecycleStartEffect(route) {
        vm.onIntent(CommuteFollowIntent.Start)
        onStopOrDispose { vm.onIntent(CommuteFollowIntent.Stop) }
    }
    val requestLocation = rememberLocationRequest(
        permissions = vm.locationPermissions,
        isGranted = vm::isLocationGranted,
        onGranted = { vm.onIntent(CommuteFollowIntent.LocateMe) },
        onDenied = { vm.onIntent(CommuteFollowIntent.LocationDenied) },
    )

    CommuteFollowContent(
        state = state,
        onBack = onBack,
        onIntent = vm::onIntent,
        onToggleLocation = {
            if (state.showsMyLocation) vm.onIntent(CommuteFollowIntent.HideMyLocation) else requestLocation()
        },
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommuteFollowContent(
    state: CommuteFollowUiState,
    onBack: () -> Unit,
    onIntent: (CommuteFollowIntent) -> Unit,
    onToggleLocation: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val background = MaterialTheme.colorScheme.background
    val now = rememberCommuteNow()
    val color = MaterialTheme.melon.transit.line(state.lineNumber)
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        TopAppBar(
            title = { Text(text = stringResource(R.string.commute_follow_title, state.lineNumber)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.commute_back),
                    )
                }
            },
            actions = {
                IconButton(onClick = { onIntent(CommuteFollowIntent.ExplainTapped) }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.commute_how_title),
                    )
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
            val receivedAt = state.followReceivedAt
            if ((state.loadFailed || state.isOutdated(now)) && receivedAt != null) {
                MelonBanner(
                    title = stringResource(R.string.commute_offline_title),
                    detail = stringResource(R.string.commute_offline_body, clockLabel(receivedAt)),
                    tone = MaterialTheme.colorScheme.outline,
                    icon = Icons.Filled.WifiOff,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }
            Headline(state = state, now = now)
            Status(state = state, now = now)

            val route = state.route
            // The location FAB sits on the map, as the design asks; showing the
            // student is never a default.
            Box(modifier = Modifier.padding(top = 2.dp)) {
                CommuteSchematicMap(
                    stops = route?.stops?.map { stop ->
                        SchematicStop(
                            id = stop.id,
                            location = stop.location,
                            kind = when (stop.id) {
                                state.stopId -> SchematicStopKind.Mine
                                route.campusStopId -> SchematicStopKind.Campus
                                else -> SchematicStopKind.Regular
                            },
                        )
                    }.orEmpty(),
                    path = route?.path.orEmpty(),
                    buses = state.routeVehicles.map { vehicle ->
                        SchematicBus(
                            id = vehicle.id,
                            location = vehicle.location,
                            heading = vehicle.heading,
                            emphasized = vehicle.id == state.vehicleId || state.vehicleId == null,
                        )
                    },
                    userLocation = state.userLocation,
                    lineColor = color,
                    height = 332.dp,
                )
                SmallFloatingActionButton(
                    onClick = onToggleLocation,
                    containerColor = if (state.showsMyLocation) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.melon.surface.card
                    },
                    contentColor = if (state.showsMyLocation) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                ) {
                    if (state.locating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Icon(
                            imageVector = if (state.showsMyLocation) {
                                Icons.Filled.LocationDisabled
                            } else {
                                Icons.Filled.MyLocation
                            },
                            contentDescription = stringResource(
                                if (state.showsMyLocation) {
                                    R.string.commute_hide_my_location
                                } else {
                                    R.string.commute_show_my_location
                                },
                            ),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Caption(stringResource(R.string.commute_follow_caption, state.lineNumber, state.routeVehicles.size))
                Caption(stringResource(R.string.commute_map_schematic))
            }

            val mineIndex = route?.indexOf(state.stopId)
            if (route != null && mineIndex != null) {
                Column(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .fillMaxWidth()
                        .commuteCard()
                        .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp),
                ) {
                    RouteLine(state = state, route = route, mineIndex = mineIndex, color = color)
                }
            }

            if (state.locationDenied) {
                MelonBanner(
                    title = stringResource(R.string.commute_location_denied_title),
                    detail = stringResource(R.string.commute_location_denied_follow_body),
                    tone = MaterialTheme.colorScheme.outline,
                    icon = Icons.Filled.LocationOff,
                    onClick = { openAppSettings(context) },
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }

    if (state.explainerOpen) {
        CommuteHowSheet(arrival = state.arrival, onDismiss = { onIntent(CommuteFollowIntent.ExplainerDismissed) })
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
        ),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun Headline(
    state: CommuteFollowUiState,
    now: Instant,
) {
    val frozen = state.isOutdated(now)
    val arrival = state.arrival
    Column(
        modifier = Modifier
            .alpha(if (arrival?.signalLost == true) 0.55f else 1f)
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when {
            arrival != null -> {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TransitEstimateText(
                        estimate = CommuteFormat.estimate(arrival, now, frozen),
                        dim = arrival.state == TransitArrivalState.Historical || frozen,
                        stale = arrival.signalLost || frozen,
                        size = 46.sp,
                    )
                    Text(
                        text = stringResource(
                            if (state.isDepartureStop) {
                                R.string.commute_follow_until_departure
                            } else {
                                R.string.commute_follow_until_your_stop
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                val detail = followDetail(arrival)
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            state.follow != null -> Text(
                text = stringResource(
                    if (state.hasPassedStop) R.string.commute_follow_passed else R.string.commute_follow_no_estimate,
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            state.loadFailed -> Text(
                text = stringResource(R.string.commute_hero_unavailable),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            else -> Box(modifier = Modifier.height(46.dp), contentAlignment = Alignment.CenterStart) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        }
    }
}

private val CommuteFollowUiState.isDepartureStop: Boolean
    get() = direction == TransitDirection.FromCampus && route?.stops?.firstOrNull()?.id == stopId

// Only a bus running this route beyond the stop has passed it; one still
// finishing the opposite trip simply has no estimate yet.
private val CommuteFollowUiState.hasPassedStop: Boolean
    get() {
        val progress = followedVehicle?.progressMeters ?: return false
        val stop = route?.stops?.firstOrNull { it.id == stopId } ?: return false
        return progress > stop.distanceMeters
    }

@Composable
private fun followDetail(arrival: TransitArrival): String? {
    val parts = mutableListOf<String>()
    // The count includes the student's stop, so 1 means it is the next one.
    val stops = arrival.stopsAway
    if (stops != null && stops > 0 && arrival.state == TransitArrivalState.Live) {
        parts += if (stops == 1) {
            stringResource(R.string.commute_follow_next_stop)
        } else {
            pluralStringResource(R.plurals.commute_follow_stops_away, stops, stops)
        }
    }
    val campus = arrival.campusArrival
    val departure = arrival.originDeparture
    if (campus != null) {
        parts += stringResource(R.string.commute_reaches_campus, clockLabel(campus))
    } else if (arrival.state == TransitArrivalState.Waiting && departure != null) {
        parts += stringResource(R.string.commute_usually_leaves, clockLabel(departure))
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun Status(
    state: CommuteFollowUiState,
    now: Instant,
) {
    val arrival = state.arrival ?: return
    Row(
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.isOutdated(now)) {
            TransitChip(kind = TransitChipKind.Stale, text = stringResource(R.string.commute_state_offline))
        } else {
            TransitChip(kind = transitChipKind(arrival))
            val fix = arrival.lastFixAt
            if (fix != null && arrival.state != TransitArrivalState.Historical) {
                TransitFreshness(fixAt = fix, now = now, signalLost = arrival.signalLost)
            }
        }
    }
}

@Composable
private fun RouteLine(
    state: CommuteFollowUiState,
    route: TransitRoute,
    mineIndex: Int,
    color: Color,
) {
    val end = if (route.direction == TransitDirection.ToCampus) {
        route.campusStopId?.let(route::indexOf) ?: route.stops.lastIndex
    } else {
        route.stops.lastIndex
    }
    val stops = route.stops.take(maxOf(end, mineIndex) + 1)
    TransitRouteProgress(
        stopNames = stops.map { transitShortName(it, route) },
        busPosition = state.followedVehicle?.progressMeters?.let { fractionalIndex(it, stops) },
        mineIndex = mineIndex,
        color = color,
    )
}

// Where `meters` falls between the stops, as a fractional stop index.
internal fun fractionalIndex(
    meters: Double,
    stops: List<TransitStop>,
): Double? {
    val first = stops.firstOrNull() ?: return null
    val last = stops.last()
    if (meters < first.distanceMeters) return null
    if (meters >= last.distanceMeters) return (stops.size - 1).toDouble()
    for (index in 0 until stops.lastIndex) {
        val from = stops[index].distanceMeters
        val to = stops[index + 1].distanceMeters
        if (meters < to) return index + if (to > from) (meters - from) / (to - from) else 0.0
    }
    return (stops.size - 1).toDouble()
}

@Preview
@Composable
private fun CommuteFollowPreview() {
    val now = Clock.System.now()
    val follow = CommuteFixtures.follow(now)
    MelonTheme {
        CommuteFollowContent(
            state = CommuteFollowUiState(
                lineNumber = "087",
                direction = TransitDirection.ToCampus,
                stopId = CommuteFixtures.MY_STOP_ID,
                arrivalId = follow.arrival?.id.orEmpty(),
                vehicleId = follow.arrival?.vehicleId,
                arrival = follow.arrival,
                follow = follow,
                followReceivedAt = now,
                route = CommuteFixtures.network.route("087", TransitDirection.ToCampus),
            ),
            onBack = {},
            onIntent = {},
            onToggleLocation = {},
        )
    }
}
