package dev.forcetower.unes.ui.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.ui.feature.me.components.AboutSheet
import dev.forcetower.unes.ui.feature.me.components.IdentityCard
import dev.forcetower.unes.ui.feature.me.components.LoggedOutView
import dev.forcetower.unes.ui.feature.me.components.LogoutFlash
import dev.forcetower.unes.ui.feature.me.components.LogoutSheet
import dev.forcetower.unes.ui.feature.me.components.MeHeader
import dev.forcetower.unes.ui.feature.me.components.MeSectionLabel
import dev.forcetower.unes.ui.feature.me.components.MeSignOutButton
import dev.forcetower.unes.ui.feature.me.components.SemesterStrip
import dev.forcetower.unes.ui.feature.me.components.SettingsCard
import dev.forcetower.unes.ui.feature.me.components.ShortcutGrid
import java.util.Locale

// "Eu" tab — personal hub mirroring the JSX `MeScreen` and iOS `MeView`.
// Stack from top to bottom: ambient mesh halo, header eyebrow + greeting,
// identity hero, semester strip, pinned shortcut grid, services list, sign-out
// pill, version footer. The screen owns three modal surfaces (about / logout
// confirmation / logout flash) and a fullscreen `LoggedOutView` swap-in once
// the logout flow lands.
@Composable
internal fun MeScreen(
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: MeViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val identity = state.identity

    // Pinned shortcut catalogue — UI affordance, not user data, so it stays
    // local. iOS makes the same call (the tweak panel that swaps presets
    // lives in Settings, not the hub itself).
    val pinnedKinds = remember { MeFixtures.defaultPinned }
    val pinned = remember(pinnedKinds) { MeFixtures.pinned(pinnedKinds) }
    val settingsRows = remember { MeFixtures.settingsRows }

    var aboutOpen by rememberSaveable { mutableStateOf(false) }

    val surface = MaterialTheme.colorScheme.surface

    Box(modifier = modifier
        .fillMaxSize()
        .background(surface)) {
        // Ambient rose mesh at the top, faded into the surface so the hero
        // sits on a soft halo rather than a hard frame.
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)) {
            Mesh(
                variant = MeshVariant.Rose,
                intensity = 0.55f,
                modifier = Modifier.fillMaxSize().padding(top = 0.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to surface,
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset),
        ) {
            MeHeader(
                identity = identity,
                modifier = Modifier.fadeUpOnAppear(delayMs = 20),
            )

            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                IdentityCard(
                    identity = identity,
                    modifier = Modifier.scaleInOnAppear(delayMs = 120, fromScale = 0.985f),
                )
                Spacer14()

                SemesterStrip(
                    identity = identity,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 220),
                )
                Spacer14()

                Column(modifier = Modifier.fadeUpOnAppear(delayMs = 300)) {
                    MeSectionLabel(
                        label = stringResource(R.string.me_section_shortcuts),
                        actionLabel = stringResource(R.string.me_section_shortcuts_action),
                        onAction = { /* manage pinned shortcuts — TODO when settings lands */ },
                    )
                    ShortcutGrid(
                        shortcuts = pinned,
                        onOpen = { /* shortcut routing — TODO once Calendar/Countdown land on Android */ },
                    )
                }
                Spacer14()

                Column(modifier = Modifier.fadeUpOnAppear(delayMs = 380)) {
                    MeSectionLabel(label = stringResource(R.string.me_section_settings))
                    SettingsCard(
                        rows = settingsRows,
                        onSelect = { id ->
                            when (id) {
                                SettingsRowKind.About -> aboutOpen = true
                                else -> Unit
                            }
                        },
                    )
                }
                Spacer14()

                MeSignOutButton(
                    onClick = { vm.onIntent(MeIntent.BeginLogout) },
                    modifier = Modifier.fadeUpOnAppear(delayMs = 460),
                )

                Footer(modifier = Modifier.fadeUpOnAppear(delayMs = 540))
            }
        }
    }

    if (state.logoutStep == LogoutStep.Confirming) {
        LogoutSheet(
            identity = identity,
            onCancel = { vm.onIntent(MeIntent.CancelLogout) },
            onConfirm = { keep -> vm.onIntent(MeIntent.ConfirmLogout(keepData = keep)) },
        )
    }

    if (aboutOpen) {
        AboutSheet(onDismiss = { aboutOpen = false })
    }

    if (state.logoutStep == LogoutStep.Flashing) {
        LogoutFlash()
    }

    if (state.logoutStep == LogoutStep.LoggedOut) {
        LoggedOutView(
            firstName = state.logoutFirstName,
            onSignIn = { vm.onIntent(MeIntent.DismissLoggedOut) },
        )
    }
}

@Composable
private fun Spacer14() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun Footer(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.me_footer).uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            letterSpacing = 1.26.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}
