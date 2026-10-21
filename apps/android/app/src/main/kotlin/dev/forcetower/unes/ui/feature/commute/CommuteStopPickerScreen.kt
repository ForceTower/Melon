package dev.forcetower.unes.ui.feature.commute

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitStop
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.MelonBanner
import dev.forcetower.unes.designsystem.components.MelonGhostButton
import dev.forcetower.unes.designsystem.components.MelonPrimaryButton
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.mvi.collectAsEffect

// Setup, step 2 (going in only): where the student actually boards. Search
// by name, tap a stop on the schematic map, or sort by distance after an
// explicit location request (dc `MeuTrajetoScreen` · Ponto; iOS
// `CommuteStopPickerView`). The radio says one stop, as the design asks.
@Composable
internal fun CommuteStopPickerScreen(
    onBack: () -> Unit,
    onGoToLines: (TransitDirection) -> Unit,
    onExpired: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CommuteSetupViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.onIntent(CommuteSetupIntent.OpenStop) }
    vm.effects.collectAsEffect { effect ->
        when (effect) {
            is CommuteSetupEffect.GoToLines -> onGoToLines(effect.direction)
            CommuteSetupEffect.Expired -> onExpired()
            CommuteSetupEffect.GoToStop, CommuteSetupEffect.Removed, is CommuteSetupEffect.Finished -> Unit
        }
    }
    val requestLocation = rememberLocationRequest(
        permissions = vm.locationPermissions,
        isGranted = vm::isLocationGranted,
        onGranted = { vm.onIntent(CommuteSetupIntent.LocateMe) },
        onDenied = { vm.onIntent(CommuteSetupIntent.LocationDenied) },
    )
    CommuteStopPickerContent(
        state = state,
        onBack = onBack,
        onIntent = vm::onIntent,
        onUseLocation = requestLocation,
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@Composable
internal fun CommuteStopPickerContent(
    state: CommuteSetupUiState,
    onBack: () -> Unit,
    onIntent: (CommuteSetupIntent) -> Unit,
    onUseLocation: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val context = LocalContext.current
    val selected = state.selectedStop
    CommuteSetupScaffold(
        step = 2,
        total = state.totalSteps,
        barTitle = stringResource(R.string.commute_direction_to_campus),
        title = stringResource(R.string.commute_setup_stop_title),
        subtitle = stringResource(R.string.commute_setup_stop_subtitle),
        onBack = onBack,
        modifier = modifier,
        bottomInset = bottomInset,
        footer = {
            MelonPrimaryButton(
                text = stringResource(
                    if (selected ==
                        null
                    ) {
                        R.string.commute_setup_stop_pick
                    } else {
                        R.string.commute_continue
                    },
                ),
                onClick = { onIntent(CommuteSetupIntent.ContinueFromStop) },
                enabled = selected != null,
            )
        },
    ) {
        StopSearchBar(query = state.query, onQueryChange = { onIntent(CommuteSetupIntent.QueryChanged(it)) })
        Spacer(Modifier.height(10.dp))
        when (state.location) {
            CommuteLocationStatus.Denied -> MelonBanner(
                title = stringResource(R.string.commute_location_denied_title),
                detail = stringResource(R.string.commute_location_denied_body),
                tone = MaterialTheme.colorScheme.outline,
                icon = Icons.Filled.LocationOff,
                onClick = { openAppSettings(context) },
            )
            else -> MelonGhostButton(
                text = stringResource(
                    when {
                        state.location == CommuteLocationStatus.Unavailable ->
                            R.string.commute_setup_location_unavailable
                        state.userLocation == null -> R.string.commute_setup_use_location
                        else -> R.string.commute_setup_near_you
                    },
                ),
                onClick = onUseLocation,
                leading = {
                    if (state.location == CommuteLocationStatus.Locating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )
        }
        val network = state.network
        when {
            network != null -> {
                val stops = state.stops
                CommuteSchematicMap(
                    stops = stops.map { stop ->
                        SchematicStop(
                            id = stop.id,
                            location = stop.location,
                            kind = if (stop.id ==
                                state.selectedStopId
                            ) {
                                SchematicStopKind.Selected
                            } else {
                                SchematicStopKind.Regular
                            },
                        )
                    },
                    userLocation = state.userLocation,
                    onStopTapped = { onIntent(CommuteSetupIntent.StopSelected(it)) },
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    text = stringResource(R.string.commute_setup_map_hint),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 7.dp),
                )
                StopList(state = state, stops = stops, onIntent = onIntent)
            }
            state.networkFailed -> MelonBanner(
                title = stringResource(R.string.commute_hero_unavailable),
                detail = stringResource(R.string.commute_setup_stop_load_failed),
                tone = MaterialTheme.colorScheme.outline,
                onClick = { onIntent(CommuteSetupIntent.LoadNetwork) },
                modifier = Modifier.padding(top = 16.dp),
            )
            else -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private const val COLLAPSED_COUNT = 8

@Composable
private fun StopList(
    state: CommuteSetupUiState,
    stops: List<TransitStop>,
    onIntent: (CommuteSetupIntent) -> Unit,
) {
    var showsAll by rememberSaveable { mutableStateOf(false) }
    val visible = if (showsAll || state.query.isNotEmpty()) stops else stops.take(COLLAPSED_COUNT)
    Column(modifier = Modifier.padding(top = 18.dp)) {
        CommuteSetupSectionLabel(
            label = stringResource(
                if (state.userLocation == null) R.string.commute_setup_stops else R.string.commute_setup_nearby_stops,
            ),
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (stops.isEmpty()) {
            Text(
                text = stringResource(R.string.commute_setup_no_match),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .commuteCard(),
            ) {
                visible.forEachIndexed { index, stop ->
                    StopRow(
                        stop = stop,
                        selected = stop.id == state.selectedStopId,
                        lines = state.network?.linesServing(stop.id, TransitDirection.ToCampus).orEmpty(),
                        distanceMeters = state.userLocation?.let { stop.location.distanceTo(it) },
                        onSelect = { onIntent(CommuteSetupIntent.StopSelected(stop.id)) },
                    )
                    if (index < visible.lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.melon.surface.line)
                    }
                }
            }
            if (visible.size < stops.size) {
                TextButton(
                    onClick = { showsAll = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.commute_setup_show_all_stops, stops.size),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StopRow(
    stop: TransitStop,
    selected: Boolean,
    lines: List<String>,
    distanceMeters: Double?,
    onSelect: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) accent.copy(alpha = 0.06f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(
                modifier = Modifier.padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val area = stop.area
                if (area != null) {
                    Text(
                        text = area,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                    )
                }
                if (distanceMeters != null) {
                    Text(
                        text = stringResource(R.string.commute_stop_distance_format, distanceLabel(distanceMeters)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        maxLines = 1,
                    )
                }
                lines.forEach { TransitLineBadge(number = it, size = TransitBadgeSize.Small) }
            }
        }
        RadioButton(selected = selected, onClick = null)
    }
}

// M3 filled search field — the `LicensesSearchBar` recipe with the stop
// placeholder: a pill with a leading magnifier and a trailing clear button.
@Composable
private fun StopSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val line = MaterialTheme.melon.surface.line
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(26.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) ink3 else line,
        animationSpec = MelonMotion.ease(),
        label = "stop-search-border",
    )
    val textStyle = TextStyle(
        fontFamily = LocalTextStyle.current.fontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = ink,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(surface)
            .border(1.dp, borderColor, shape)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = ink3,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(text = stringResource(R.string.commute_setup_stop_search), style = textStyle.copy(color = ink3))
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(accent),
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            val description = stringResource(R.string.commute_setup_stop_search_clear)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = { onQueryChange("") })
                    .semantics {
                        role = Role.Button
                        contentDescription = description
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = ink3,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun CommuteStopPickerPreview() {
    MelonTheme {
        CommuteStopPickerContent(
            state = CommuteSetupUiState(
                directions = TransitDirection.entries,
                network = CommuteFixtures.network,
                selectedStopId = CommuteFixtures.MY_STOP_ID,
            ),
            onBack = {},
            onIntent = {},
            onUseLocation = {},
        )
    }
}
