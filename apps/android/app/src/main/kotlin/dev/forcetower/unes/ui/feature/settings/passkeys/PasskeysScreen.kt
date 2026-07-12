package dev.forcetower.unes.ui.feature.settings.passkeys

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.PinnedHeaderHairline
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon

// "Chaves de acesso" (dc `PasskeysScreen`), pushed from the Configurações
// credential vault. Lists the account's WebAuthn credentials, adds one through
// the system CredentialManager sheet (choose → confirm → done), and renames or
// revokes an existing one. Same pinned-back-bar + scrolling-headline chrome as
// SettingsScreen.
@Composable
internal fun PasskeysScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: PasskeysViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    // `hiltViewModel()` resolves to the activity store here, so the ViewModel
    // outlives the screen and its `init` load runs only once. Re-sync on every
    // appearance (silent — no spinner) so re-opening reflects keys added or
    // revoked elsewhere, matching the Settings row's refresh.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.onIntent(PasskeysIntent.Refresh) }

    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 0 } }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            PasskeysTopBar(onBack = onBack, modifier = Modifier.fadeUpOnAppear(delayMs = 20))
            PinnedHeaderHairline(scrolled = scrolled)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = bottomInset + 16.dp),
            ) {
                PasskeysHeader(modifier = Modifier.fadeUpOnAppear(delayMs = 40))

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    when {
                        state.loading -> PasskeysStatusCard(loading = true)
                        state.loadError -> PasskeysStatusCard(
                            loading = false,
                            message = stringResource(R.string.passkeys_load_error),
                            actionLabel = stringResource(R.string.passkeys_retry),
                            onAction = { vm.onIntent(PasskeysIntent.Load) },
                        )
                        state.isEmpty -> PasskeysEmptyCard(modifier = Modifier.fadeUpOnAppear(delayMs = 80))
                        else -> PasskeyListCard(
                            items = state.items,
                            newlyAddedId = state.newlyAddedId,
                            onOpen = { vm.onIntent(PasskeysIntent.OpenDetail(it)) },
                            modifier = Modifier.fadeUpOnAppear(delayMs = 80),
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    AddPasskeyButton(
                        onClick = { vm.onIntent(PasskeysIntent.OpenAdd) },
                        modifier = Modifier.fadeUpOnAppear(delayMs = 140),
                    )
                    PasskeysFootnote(modifier = Modifier.fadeUpOnAppear(delayMs = 200))
                }
            }
        }

        // Overlays: add + detail sheets and the delete dialog. Kept
        // always-composed (inside the stacking Box) so their slide/fade
        // transitions play on both enter and exit and sit above the content.
        AddPasskeySheet(
            state = state,
            activity = activity,
            onIntent = vm::onIntent,
        )
        PasskeyDetailSheet(
            state = state,
            onIntent = vm::onIntent,
        )
        DeletePasskeyDialog(
            state = state,
            onIntent = vm::onIntent,
        )

        PasskeyToastHost(
            toast = state.toast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset + 24.dp),
        )
    }
}

@Composable
private fun PasskeysTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .offset(x = (-10).dp)
                .size(44.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.passkeys_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun PasskeysHeader(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp, bottom = 18.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Key,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.passkeys_title),
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 32.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.64).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.passkeys_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun PasskeyListCard(
    items: List<PasskeyItem>,
    newlyAddedId: String?,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card),
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.melon.surface.line),
                )
            }
            PasskeyRow(
                item = item,
                highlighted = item.id == newlyAddedId,
                onClick = { onOpen(item.id) },
            )
        }
    }
}

@Composable
private fun PasskeyRow(item: PasskeyItem, highlighted: Boolean, onClick: () -> Unit) {
    val flash by animateColorAsState(
        targetValue = if (highlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            Color.Transparent
        },
        label = "passkeyFlash",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .background(flash)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        PasskeyTile(isSynced = item.isSynced, size = 40.dp, radius = 12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name ?: stringResource(R.string.passkeys_name_fallback),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            SyncLine(isSynced = item.isSynced, trailing = item.createdAtLabel)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
internal fun SyncLine(isSynced: Boolean, trailing: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = if (isSynced) Icons.Filled.CloudDone else Icons.Filled.Lock,
            contentDescription = null,
            tint = syncColor(isSynced),
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(
                if (isSynced) R.string.passkeys_sync_synced else R.string.passkeys_sync_local,
            ),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.outline,
        )
        if (!trailing.isNullOrBlank()) {
            Text(
                text = "·",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PasskeysEmptyCard(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .padding(horizontal = 24.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.passkeys_empty_title),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.passkeys_empty_body),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PasskeysStatusCard(
    loading: Boolean,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .padding(horizontal = 24.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp),
            )
        } else {
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onAction)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun AddPasskeyButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = stringResource(R.string.passkeys_add),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun PasskeysFootnote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.VerifiedUser,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = stringResource(R.string.passkeys_footnote),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun PasskeyToastHost(toast: PasskeyToast?, modifier: Modifier = Modifier) {
    // Hold the last shown toast so the fade-out keeps its own copy + tint
    // instead of flashing the default while `toast` is already null.
    var lastToast by remember { mutableStateOf(PasskeyToast.Created) }
    if (toast != null) lastToast = toast

    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut(),
        modifier = modifier,
    ) {
        val resolved = lastToast
        val isError = resolved == PasskeyToast.Error
        val icon = when (resolved) {
            PasskeyToast.Deleted -> Icons.Filled.Delete
            PasskeyToast.Error -> Icons.Filled.ErrorOutline
            else -> Icons.Filled.CheckCircle
        }
        val message = stringResource(
            when (resolved) {
                PasskeyToast.Created -> R.string.passkeys_toast_created
                PasskeyToast.Renamed -> R.string.passkeys_toast_renamed
                PasskeyToast.Deleted -> R.string.passkeys_toast_deleted
                PasskeyToast.Error -> R.string.passkeys_toast_error
            },
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isError) MaterialTheme.melon.status.bad else MaterialTheme.melon.fixed.success,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}

// ── Shared tokens (also used by the sheets, same package) ──

@Composable
internal fun syncColor(isSynced: Boolean): Color =
    if (isSynced) MaterialTheme.melon.fixed.success else MaterialTheme.colorScheme.outline

@Composable
internal fun passkeyTileTone(isSynced: Boolean): Color =
    if (isSynced) MaterialTheme.melon.palette.violet else MaterialTheme.melon.palette.orange

@Composable
internal fun passkeyTileIcon(isSynced: Boolean): ImageVector =
    if (isSynced) Icons.Filled.Smartphone else Icons.Filled.VpnKey

// Rounded tinted tile carrying the credential glyph. Shared by the list row,
// the add-target cards, and the detail header.
@Composable
internal fun PasskeyTile(isSynced: Boolean, size: Dp, radius: Dp) {
    val tone = passkeyTileTone(isSynced)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(tone),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = passkeyTileIcon(isSynced),
            contentDescription = null,
            tint = MaterialTheme.melon.fixed.onHero,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}

@Preview
@Composable
private fun PasskeysScreenPreview() {
    MelonTheme { PasskeysScreen(onBack = {}) }
}
