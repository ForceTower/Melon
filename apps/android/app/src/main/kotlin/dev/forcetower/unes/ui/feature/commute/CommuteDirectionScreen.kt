package dev.forcetower.unes.ui.feature.commute

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.MelonBanner
import dev.forcetower.unes.designsystem.components.MelonPrimaryButton
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.mvi.collectAsEffect

// Setup, step 1: which trips the student wants help with. Also the way back
// into setup from "Meu trajeto", where it offers removing the plan (dc
// `MeuTrajetoScreen` · Direção; iOS `CommuteDirectionView`).
@Composable
internal fun CommuteDirectionScreen(
    onBack: () -> Unit,
    onGoToStop: () -> Unit,
    onGoToLines: (TransitDirection) -> Unit,
    onRemoved: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CommuteSetupViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.onIntent(CommuteSetupIntent.Enter) }
    vm.effects.collectAsEffect { effect ->
        when (effect) {
            CommuteSetupEffect.GoToStop -> onGoToStop()
            is CommuteSetupEffect.GoToLines -> onGoToLines(effect.direction)
            CommuteSetupEffect.Removed -> onRemoved()
            is CommuteSetupEffect.Finished -> Unit
        }
    }
    CommuteDirectionContent(
        state = state,
        onBack = onBack,
        onIntent = vm::onIntent,
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@Composable
internal fun CommuteDirectionContent(
    state: CommuteSetupUiState,
    onBack: () -> Unit,
    onIntent: (CommuteSetupIntent) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    CommuteSetupScaffold(
        step = 1,
        total = state.totalSteps,
        barTitle = stringResource(R.string.commute_title),
        title = stringResource(R.string.commute_setup_direction_title),
        subtitle = stringResource(R.string.commute_setup_direction_subtitle),
        onBack = onBack,
        modifier = modifier,
        bottomInset = bottomInset,
        footer = {
            MelonPrimaryButton(
                text = stringResource(R.string.commute_continue),
                onClick = { onIntent(CommuteSetupIntent.ContinueFromDirection) },
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DirectionOption(
                direction = TransitDirection.ToCampus,
                icon = Icons.AutoMirrored.Filled.Login,
                bodyRes = R.string.commute_setup_to_campus_body,
                selected = TransitDirection.ToCampus in state.directions,
                onToggle = { onIntent(CommuteSetupIntent.DirectionToggled(TransitDirection.ToCampus)) },
            )
            DirectionOption(
                direction = TransitDirection.FromCampus,
                icon = Icons.AutoMirrored.Filled.Logout,
                bodyRes = R.string.commute_setup_from_campus_body,
                selected = TransitDirection.FromCampus in state.directions,
                onToggle = { onIntent(CommuteSetupIntent.DirectionToggled(TransitDirection.FromCampus)) },
            )
        }
        MelonBanner(
            title = stringResource(R.string.commute_works_without_location),
            detail = stringResource(R.string.commute_setup_no_location_needed),
            tone = MaterialTheme.colorScheme.outline,
            icon = Icons.Filled.LocationOff,
            modifier = Modifier.padding(top = 18.dp),
        )
        if (state.isEditing) {
            Spacer(Modifier.height(22.dp))
            TextButton(
                onClick = { onIntent(CommuteSetupIntent.RemoveTapped) },
                enabled = !state.removing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.removing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = stringResource(R.string.commute_setup_remove),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.melon.fixed.destructive,
                    )
                }
            }
        }
    }

    if (state.removeConfirmOpen) {
        AlertDialog(
            onDismissRequest = { onIntent(CommuteSetupIntent.RemoveDismissed) },
            title = { Text(text = stringResource(R.string.commute_setup_remove_confirm_title)) },
            text = { Text(text = stringResource(R.string.commute_setup_remove_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { onIntent(CommuteSetupIntent.RemoveConfirmed) }) {
                    Text(
                        text = stringResource(R.string.commute_setup_remove),
                        color = MaterialTheme.melon.fixed.destructive,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(CommuteSetupIntent.RemoveDismissed) }) {
                    Text(text = stringResource(R.string.commute_setup_cancel))
                }
            },
        )
    }
    if (state.removeFailed) {
        AlertDialog(
            onDismissRequest = { onIntent(CommuteSetupIntent.RemoveFailureDismissed) },
            title = { Text(text = stringResource(R.string.commute_setup_remove_failed)) },
            text = { Text(text = stringResource(R.string.commute_setup_try_again_body)) },
            confirmButton = {
                TextButton(onClick = { onIntent(CommuteSetupIntent.RemoveFailureDismissed) }) {
                    Text(text = stringResource(R.string.commute_ok))
                }
            },
        )
    }
}

// A direction card: tonal icon tile, title + body, and an M3 checkbox — the
// design's "checkbox for what can be more than one".
@Composable
private fun DirectionOption(
    direction: TransitDirection,
    icon: ImageVector,
    bodyRes: Int,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.08f) else MaterialTheme.melon.surface.card)
            .border(1.5.dp, if (selected) accent else MaterialTheme.melon.surface.line, shape)
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(direction.labelRes),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.5.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Checkbox(checked = selected, onCheckedChange = null)
    }
}

@Preview
@Composable
private fun CommuteDirectionPreview() {
    MelonTheme {
        CommuteDirectionContent(
            state = CommuteSetupUiState(existing = CommuteFixtures.plan, directions = TransitDirection.entries),
            onBack = {},
            onIntent = {},
        )
    }
}
