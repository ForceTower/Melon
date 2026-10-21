package dev.forcetower.unes.ui.feature.commute

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.transit.domain.model.CommutePlan
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitLine
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.MelonBanner
import dev.forcetower.unes.designsystem.components.MelonPrimaryButton
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.mvi.collectAsEffect
import kotlin.math.roundToInt

// Setup, last step per direction: the lines the student really takes at that
// stop and how long the walk there is. Leaving campus has a single boarding
// stop, so that direction comes straight here (dc `MeuTrajetoScreen` ·
// Linhas; iOS `CommuteLinesView`). The "me avise na hora de sair" switch
// from the design is the notifications follow-up and isn't here yet.
@Composable
internal fun CommuteLinesScreen(
    direction: TransitDirection,
    onBack: () -> Unit,
    onGoToLines: (TransitDirection) -> Unit,
    onFinished: (CommutePlan, TransitDirection) -> Unit,
    onExpired: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CommuteSetupViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(direction) { vm.onIntent(CommuteSetupIntent.OpenLines(direction)) }
    vm.effects.collectAsEffect { effect ->
        when (effect) {
            is CommuteSetupEffect.GoToLines -> onGoToLines(effect.direction)
            is CommuteSetupEffect.Finished -> onFinished(effect.plan, effect.direction)
            CommuteSetupEffect.Expired -> onExpired()
            CommuteSetupEffect.GoToStop, CommuteSetupEffect.Removed -> Unit
        }
    }
    CommuteLinesContent(
        state = state,
        direction = direction,
        onBack = onBack,
        onIntent = vm::onIntent,
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@Composable
internal fun CommuteLinesContent(
    state: CommuteSetupUiState,
    direction: TransitDirection,
    onBack: () -> Unit,
    onIntent: (CommuteSetupIntent) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val toCampus = direction == TransitDirection.ToCampus
    // The catalog answered and the session is on this step's direction.
    val network = state.network?.takeIf { state.linesDirection == direction }
    val stop = state.linesStop?.takeIf { state.linesDirection == direction }
    val ready = network != null && stop != null
    CommuteSetupScaffold(
        step = state.linesStep(direction),
        total = state.totalSteps,
        barTitle = stop?.name.orEmpty(),
        title = stringResource(
            if (toCampus) R.string.commute_setup_lines_title else R.string.commute_setup_lines_title_leaving,
        ),
        subtitle = stringResource(R.string.commute_setup_lines_subtitle),
        onBack = onBack,
        modifier = modifier,
        bottomInset = bottomInset,
        footer = {
            MelonPrimaryButton(
                text = stringResource(
                    if (state.isLastLinesStep) R.string.commute_setup_save else R.string.commute_continue,
                ),
                onClick = { onIntent(CommuteSetupIntent.SaveTapped) },
                enabled = ready && state.selectedLines.isNotEmpty() && !state.saving,
                isLoading = state.saving,
            )
        },
    ) {
        when {
            network != null && stop != null -> {
                LinesCard(state = state, direction = direction, network = network, onIntent = onIntent)
                WalkCard(state = state, direction = direction, onIntent = onIntent)
            }
            state.networkFailed -> MelonBanner(
                title = stringResource(R.string.commute_hero_unavailable),
                detail = stringResource(R.string.commute_setup_stop_load_failed),
                tone = MaterialTheme.colorScheme.outline,
                onClick = { onIntent(CommuteSetupIntent.LoadNetwork) },
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

    if (state.saveFailed) {
        AlertDialog(
            onDismissRequest = { onIntent(CommuteSetupIntent.SaveFailureDismissed) },
            title = { Text(text = stringResource(R.string.commute_setup_save_failed)) },
            text = { Text(text = stringResource(R.string.commute_setup_try_again_body)) },
            confirmButton = {
                TextButton(onClick = { onIntent(CommuteSetupIntent.SaveFailureDismissed) }) {
                    Text(text = stringResource(R.string.commute_ok))
                }
            },
        )
    }
}

@Composable
private fun LinesCard(
    state: CommuteSetupUiState,
    direction: TransitDirection,
    network: dev.forcetower.melon.feature.transit.domain.model.TransitNetwork,
    onIntent: (CommuteSetupIntent) -> Unit,
) {
    val serving = state.servingLines
    val ordered = network.lines.filter { it.number in serving } + network.lines.filter { it.number !in serving }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .commuteCard(),
    ) {
        ordered.forEachIndexed { index, line ->
            LineRow(
                line = line,
                serves = line.number in serving,
                selected = line.number in state.selectedLines,
                typical = network.route(line.number, direction)?.typicalMinutes,
                onToggle = { onIntent(CommuteSetupIntent.LineToggled(line.number)) },
            )
            if (index < ordered.lastIndex) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.melon.surface.line)
            }
        }
    }
}

@Composable
private fun LineRow(
    line: TransitLine,
    serves: Boolean,
    selected: Boolean,
    typical: IntRange?,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (serves) 1f else 0.45f)
            .clickable(enabled = serves, role = Role.Checkbox, onClick = onToggle)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TransitLineBadge(number = line.number, size = TransitBadgeSize.Large, ghost = !selected)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = when {
                    !serves -> stringResource(R.string.commute_setup_line_does_not_serve)
                    typical == null -> stringResource(R.string.commute_setup_line_serves)
                    typical.first == typical.last -> stringResource(R.string.commute_setup_line_duration, typical.first)
                    else -> stringResource(R.string.commute_setup_line_duration_range, typical.first, typical.last)
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (serves) Checkbox(checked = selected, onCheckedChange = null) else Spacer(Modifier.width(48.dp))
    }
}

// The walk to the stop on an M3 slider, the value beside the label.
@Composable
private fun WalkCard(
    state: CommuteSetupUiState,
    direction: TransitDirection,
    onIntent: (CommuteSetupIntent) -> Unit,
) {
    val range = CommuteSetupViewModel.WALK_RANGE
    Column(modifier = Modifier.padding(top = 22.dp)) {
        CommuteSetupSectionLabel(
            label = stringResource(R.string.commute_setup_walk_title),
            hint = stringResource(
                if (direction == TransitDirection.ToCampus) {
                    R.string.commute_setup_walk_hint
                } else {
                    R.string.commute_setup_walk_hint_leaving
                },
            ),
        )
        Column(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .commuteCard()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(21.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.commute_setup_walk_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.commute_setup_walk_minutes_value, state.walkMinutes),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Slider(
                value = state.walkMinutes.toFloat(),
                onValueChange = { onIntent(CommuteSetupIntent.WalkChanged(it.roundToInt())) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.commute_setup_walk_minutes_value, range.first),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    text = stringResource(R.string.commute_setup_walk_minutes_value, range.last),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Preview
@Composable
private fun CommuteLinesPreview() {
    val network = CommuteFixtures.network
    val stop = network.route("087", TransitDirection.ToCampus)?.stops?.get(4)
    MelonTheme {
        CommuteLinesContent(
            state = CommuteSetupUiState(
                directions = listOf(TransitDirection.ToCampus),
                network = network,
                linesDirection = TransitDirection.ToCampus,
                linesStop = stop,
                selectedLines = listOf("087", "088"),
            ),
            direction = TransitDirection.ToCampus,
            onBack = {},
            onIntent = {},
        )
    }
}
