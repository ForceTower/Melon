package dev.forcetower.unes.ui.feature.me

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.ui.feature.connected.ConnectedRoute
import dev.forcetower.unes.ui.feature.connected.LocalConnectedNavigator
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
import dev.forcetower.unes.ui.feature.me.components.rememberAppInfo
import java.util.Locale

// "Eu" tab — personal hub mirroring the JSX `MeScreen` and iOS `MeView`.
// Stack from top to bottom: ambient mesh halo, header eyebrow + greeting,
// identity hero, semester strip, pinned shortcut grid, services list, sign-out
// pill, version footer. The screen owns three modal surfaces (about / logout
// confirmation / logout flash) and a fullscreen `LoggedOutView` swap-in once
// the logout flow lands.
@Composable
internal fun MeScreen(
    onLoggedOut: () -> Unit,
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

    val context = LocalContext.current
    val connectedNavigator = LocalConnectedNavigator.current
    val appInfo = rememberAppInfo()
    val feedbackRecipient = stringResource(R.string.me_feedback_recipient)
    val feedbackBody = stringResource(
        R.string.me_feedback_body_format,
        appInfo.version,
        appInfo.build,
        appInfo.phoneModel,
        Locale.getDefault().toLanguageTag(),
        appInfo.machineId,
    )

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
                intensity = 0.22f,
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
            if (identity != null) {
                MeHeader(
                    identity = identity,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 20),
                )
            }

            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                if (identity != null) {
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
                }

                Column(modifier = Modifier.fadeUpOnAppear(delayMs = 300)) {
                    MeSectionLabel(label = stringResource(R.string.me_section_shortcuts))
                    ShortcutGrid(
                        shortcuts = pinned,
                        onOpen = { kind ->
                            when (kind) {
                                ShortcutKind.Calendar -> connectedNavigator.navigate(ConnectedRoute.Calendar)
                                ShortcutKind.Countdown -> connectedNavigator.navigate(ConnectedRoute.FinalCountdown)
                                else -> Unit
                            }
                        },
                    )
                }
                Spacer14()

                Column(modifier = Modifier.fadeUpOnAppear(delayMs = 380)) {
                    MeSectionLabel(label = stringResource(R.string.me_section_settings))
                    SettingsCard(
                        rows = settingsRows,
                        onSelect = { id ->
                            when (id) {
                                SettingsRowKind.Settings -> connectedNavigator.navigate(ConnectedRoute.Settings)
                                SettingsRowKind.About -> aboutOpen = true
                                SettingsRowKind.Feedback -> launchFeedbackEmail(
                                    context = context,
                                    recipient = feedbackRecipient,
                                    body = feedbackBody,
                                )
                                SettingsRowKind.Licenses -> connectedNavigator.navigate(ConnectedRoute.Licenses)
                            }
                        },
                    )
                }
                Spacer14()

                MeSignOutButton(
                    onClick = { vm.onIntent(MeIntent.BeginLogout) },
                    modifier = Modifier.fadeUpOnAppear(delayMs = 460),
                )

                Footer(
                    version = appInfo.version,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 540),
                )
            }
        }
    }

    if (state.logoutStep == LogoutStep.Confirming && identity != null) {
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
            // Reset *before* bubbling the nav callback up: Hilt scopes
            // MeViewModel to the Activity, so without this the VM sticks at
            // `LoggedOut` and the goodbye view re-appears the next time the
            // Me tab mounts after the user signs back in.
            onSignIn = {
                vm.onIntent(MeIntent.ResetLogout)
                onLoggedOut()
            },
        )
    }
}

@Composable
private fun Spacer14() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun Footer(version: String, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.me_footer_format, version).uppercase(Locale.ROOT),
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

// Fires a `mailto:` SEND_TO intent — Android's analogue to iOS's
// `sms:joaopaulo761@gmail.com?body=…` (which on iOS routes through iMessage
// since the recipient is an email-as-iMessage-id). On Android there's no
// iMessage equivalent, so feedback goes through the user's email client; the
// debug-info body iOS attaches survives the platform swap unchanged.
private fun launchFeedbackEmail(
    context: android.content.Context,
    recipient: String,
    body: String,
) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No email client installed — drop silently, same as iOS when
        // `canOpenURL` rejects the sms: URL.
    }
}
