package dev.forcetower.unes.ui.feature.settings.passkeys

import android.app.Activity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon

// ── Bottom-sheet scaffold ──

// A real Material 3 ModalBottomSheet: it renders in its own window, so it sits
// above the Connected shell's bottom navigation bar (like Materiais does). A
// plain in-content overlay would be clipped to the NavDisplay region and leave
// the nav bar showing through. Themed to the always-dark vault card.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasskeyModalSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.melon.surface.card,
        contentColor = MaterialTheme.colorScheme.onBackground,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
        ) {
            content()
        }
    }
}

// ── Add sheet ──

@Composable
internal fun AddPasskeySheet(
    state: PasskeysUiState,
    activity: Activity?,
    onIntent: (PasskeysIntent) -> Unit,
) {
    if (state.sheet != PasskeySheet.Add) return
    PasskeyModalSheet(onDismiss = { onIntent(PasskeysIntent.CloseSheet) }) {
        Crossfade(targetState = state.addStep, label = "addStep") { step ->
            when (step) {
                AddStep.Choose -> AddChooseStep(state = state, activity = activity, onIntent = onIntent)
                AddStep.Auth -> AddAuthStep()
                AddStep.Success -> AddSuccessStep()
            }
        }
    }
}

@Composable
private fun AddChooseStep(
    state: PasskeysUiState,
    activity: Activity?,
    onIntent: (PasskeysIntent) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.passkeys_add_title),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.passkeys_add_subtitle),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            TargetCard(
                target = PasskeyTarget.ThisDevice,
                title = stringResource(R.string.passkeys_target_device_title),
                subtitle = stringResource(R.string.passkeys_target_device_sub),
                selected = state.target == PasskeyTarget.ThisDevice,
                onSelect = { onIntent(PasskeysIntent.SelectTarget(PasskeyTarget.ThisDevice)) },
            )
            TargetCard(
                target = PasskeyTarget.SecurityKey,
                title = stringResource(R.string.passkeys_target_key_title),
                subtitle = stringResource(R.string.passkeys_target_key_sub),
                selected = state.target == PasskeyTarget.SecurityKey,
                onSelect = { onIntent(PasskeysIntent.SelectTarget(PasskeyTarget.SecurityKey)) },
            )
        }
        Spacer(Modifier.height(18.dp))
        SheetPrimaryButton(
            text = stringResource(R.string.passkeys_add_continue),
            enabled = activity != null,
            onClick = { activity?.let { onIntent(PasskeysIntent.ContinueAdd(it)) } },
        )
        Spacer(Modifier.height(4.dp))
        SheetTextButton(
            text = stringResource(R.string.passkeys_add_cancel),
            color = MaterialTheme.colorScheme.outline,
            onClick = { onIntent(PasskeysIntent.CloseSheet) },
        )
    }
}

@Composable
private fun TargetCard(
    target: PasskeyTarget,
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)
    val background = if (selected) {
        accent.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val borderColor = if (selected) accent else MaterialTheme.melon.surface.line
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.5.dp, borderColor, shape)
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        PasskeyTile(isSynced = target == PasskeyTarget.ThisDevice, size = 36.dp, radius = 10.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        RadioDot(selected = selected)
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) accent else Color.Transparent)
            .border(2.dp, if (selected) accent else MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun AddAuthStep() {
    val accent = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "authPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scale",
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(112.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size((88 * scale).dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(2.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(28.dp)),
            )
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(50.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.passkeys_auth_title),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.passkeys_auth_subtitle),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun AddSuccessStep() {
    val success = MaterialTheme.melon.fixed.success
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .size(76.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(success),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.melon.fixed.onHero,
                modifier = Modifier.size(42.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.passkeys_success_title),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.passkeys_success_subtitle),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(10.dp))
    }
}

// ── Detail sheet ──

@Composable
internal fun PasskeyDetailSheet(
    state: PasskeysUiState,
    onIntent: (PasskeysIntent) -> Unit,
) {
    if (state.sheet != PasskeySheet.Detail) return
    val item = state.detail ?: return
    PasskeyModalSheet(onDismiss = { onIntent(PasskeysIntent.CloseSheet) }) {
        DetailContent(item = item, state = state, onIntent = onIntent)
    }
}

@Composable
private fun DetailContent(
    item: PasskeyItem,
    state: PasskeysUiState,
    onIntent: (PasskeysIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        PasskeyTile(isSynced = item.isSynced, size = 64.dp, radius = 18.dp)
        Spacer(Modifier.height(14.dp))

        if (state.editing) {
            RenameField(
                value = state.editName,
                saving = state.savingName,
                onValue = { onIntent(PasskeysIntent.EditNameChanged(it)) },
                onSave = { onIntent(PasskeysIntent.SaveName) },
            )
        } else {
            Text(
                text = item.name ?: stringResource(R.string.passkeys_name_fallback),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            SyncLine(isSynced = item.isSynced, trailing = null)
        }
    }

    if (!state.editing) {
        Spacer(Modifier.height(18.dp))
        DetailInfoCard(item = item)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedActionButton(
                text = stringResource(R.string.passkeys_rename),
                icon = Icons.Filled.Edit,
                modifier = Modifier.weight(1f),
                onClick = { onIntent(PasskeysIntent.StartEdit) },
            )
            DangerActionButton(
                text = stringResource(R.string.passkeys_delete),
                icon = Icons.Filled.Delete,
                modifier = Modifier.weight(1f),
                onClick = { onIntent(PasskeysIntent.RequestDelete) },
            )
        }
    }
}

@Composable
private fun DetailInfoCard(item: PasskeyItem) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp),
    ) {
        DetailInfoRow(
            icon = Icons.Filled.Schedule,
            label = stringResource(R.string.passkeys_detail_created),
            value = item.createdAtLabel,
            divider = true,
        )
        DetailInfoRow(
            icon = if (item.isSynced) Icons.Filled.CloudDone else Icons.Filled.Lock,
            label = stringResource(R.string.passkeys_detail_sync),
            value = stringResource(
                if (item.isSynced) R.string.passkeys_sync_synced else R.string.passkeys_sync_local,
            ),
            divider = false,
        )
    }
}

@Composable
private fun DetailInfoRow(icon: ImageVector, label: String, value: String, divider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (divider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.melon.surface.line),
            )
        }
    }
}

@Composable
private fun RenameField(
    value: String,
    saving: Boolean,
    onValue: (String) -> Unit,
    onSave: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = Modifier.fillMaxWidth(fraction = 0.9f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val shape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .border(1.5.dp, accent, shape)
                .padding(horizontal = 13.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(accent)
                .clickable(enabled = !saving, role = Role.Button, onClick = onSave)
                .padding(horizontal = 16.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (saving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.passkeys_rename_save),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

// ── Delete dialog ──

@Composable
internal fun DeletePasskeyDialog(
    state: PasskeysUiState,
    onIntent: (PasskeysIntent) -> Unit,
) {
    val item = state.confirmTarget ?: return
    val danger = MaterialTheme.melon.status.bad
    Dialog(
        onDismissRequest = { onIntent(PasskeysIntent.CancelDelete) },
        properties = DialogProperties(dismissOnClickOutside = !state.deleting),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.melon.surface.card)
                .border(1.dp, MaterialTheme.melon.surface.line, RoundedCornerShape(28.dp))
                .padding(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(danger.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = danger,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(
                    R.string.passkeys_delete_title,
                    item.name ?: stringResource(R.string.passkeys_name_fallback),
                ),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = stringResource(
                    if (item.isSynced) R.string.passkeys_delete_body_synced else R.string.passkeys_delete_body_local,
                ),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp, lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SheetTextButton(
                    text = stringResource(R.string.passkeys_delete_cancel),
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onIntent(PasskeysIntent.CancelDelete) },
                    fill = false,
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(danger.copy(alpha = 0.12f))
                        .clickable(enabled = !state.deleting, role = Role.Button) {
                            onIntent(PasskeysIntent.ConfirmDelete)
                        }
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.deleting) {
                        CircularProgressIndicator(color = danger, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            text = stringResource(R.string.passkeys_delete_confirm),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = danger,
                        )
                    }
                }
            }
        }
    }
}

// ── Shared buttons ──

@Composable
private fun SheetPrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(if (enabled) accent else accent.copy(alpha = 0.5f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun SheetTextButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    fill: Boolean = true,
) {
    Box(
        modifier = Modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
}

@Composable
private fun OutlinedActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .border(1.5.dp, MaterialTheme.melon.surface.line, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun DangerActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val danger = MaterialTheme.melon.status.bad
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(danger.copy(alpha = 0.12f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = danger, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = danger,
        )
    }
}
