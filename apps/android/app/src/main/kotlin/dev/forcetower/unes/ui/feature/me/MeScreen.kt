package dev.forcetower.unes.ui.feature.me

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.me.domain.model.AcademicDocument
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.melon
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
import dev.forcetower.unes.ui.feature.me.components.SemesterProgressCard
import dev.forcetower.unes.ui.feature.me.components.SettingsCard
import dev.forcetower.unes.ui.feature.me.components.ShortcutGrid
import dev.forcetower.unes.ui.feature.me.components.WideShortcutCard
import dev.forcetower.unes.ui.feature.me.components.rememberAppInfo
import dev.forcetower.unes.ui.feature.me.documents.MeDocumentSheet
import java.util.Locale

// "Eu" tab — 2026 redesign (dc project `UNES Eu - Android`). Stack from top
// to bottom: M3 large header, identity mesh card with the
// Score · Frequência · Semestre stats, semester linear-progress card, tonal
// shortcut grid (+ the wide Materiais card), the Definições list, the
// sign-out pill, and the version footer. Shortcut visibility mirrors the iOS
// remote-config gates. The screen owns three modal surfaces (about / logout
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

    val openShortcut: (ShortcutKind) -> Unit = { kind ->
        when (kind) {
            ShortcutKind.Calendar -> connectedNavigator.navigate(ConnectedRoute.Calendar)
            ShortcutKind.Countdown -> connectedNavigator.navigate(ConnectedRoute.FinalCountdown())
            ShortcutKind.Certificate ->
                vm.onIntent(MeIntent.OpenDocument(AcademicDocument.EnrollmentCertificate))
            ShortcutKind.History ->
                vm.onIntent(MeIntent.OpenDocument(AcademicDocument.AcademicHistory))
            // Gated tiles without an Android screen yet (Matrícula, Paradoxo,
            // Materiais) — they only render in debug builds or once the
            // remote flag flips, and routing lands together with each feature.
            else -> Unit
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = bottomInset),
    ) {
        MeHeader(modifier = Modifier.fadeUpOnAppear(delayMs = 20, fromOffset = (-10).dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            if (identity != null) {
                IdentityCard(
                    identity = identity,
                    modifier = Modifier.scaleInOnAppear(delayMs = 120, fromScale = 0.97f),
                )
                if (identity.semesterTotalWeeks > 0) {
                    Spacer(Modifier.height(14.dp))
                    SemesterProgressCard(
                        identity = identity,
                        modifier = Modifier.fadeUpOnAppear(delayMs = 200),
                    )
                }
                Spacer(Modifier.height(26.dp))
            }

            Column(modifier = Modifier.fadeUpOnAppear(delayMs = 280)) {
                MeSectionLabel(label = stringResource(R.string.me_section_shortcuts))
                ShortcutGrid(shortcuts = state.shortcuts, onOpen = openShortcut)
                val materials = state.materialsShortcut
                if (materials != null) {
                    Spacer(Modifier.height(12.dp))
                    WideShortcutCard(shortcut = materials, onOpen = openShortcut)
                }
            }
            Spacer(Modifier.height(26.dp))

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
            Spacer(Modifier.height(16.dp))

            MeSignOutButton(
                onClick = { vm.onIntent(MeIntent.BeginLogout) },
                modifier = Modifier.fadeUpOnAppear(delayMs = 460),
            )

            Footer(
                version = appInfo.version,
                build = appInfo.build,
                modifier = Modifier.fadeUpOnAppear(delayMs = 540),
            )
        }
    }

    val documentSheet = state.documentSheet
    if (documentSheet != null) {
        MeDocumentSheet(
            sheet = documentSheet,
            identity = identity,
            captchaSiteKey = state.gates.documentCaptchaSiteKey,
            captchaBaseUrl = state.gates.documentCaptchaBaseUrl,
            onRequest = { vm.onIntent(MeIntent.RequestDocument) },
            onCaptchaSolved = { token -> vm.onIntent(MeIntent.CaptchaSolved(token)) },
            onCaptchaCanceled = { vm.onIntent(MeIntent.CaptchaCanceled) },
            onDismiss = { vm.onIntent(MeIntent.CloseDocument) },
        )
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
private fun Footer(version: String, build: String, modifier: Modifier = Modifier) {
    val credit = stringResource(R.string.me_footer_credit)
    val heart = "♥"
    Text(
        text = buildAnnotatedString {
            append(stringResource(R.string.me_footer_version_format, version, build))
            append("\n")
            val heartIndex = credit.indexOf(heart)
            if (heartIndex >= 0) {
                append(credit.substring(0, heartIndex))
                withStyle(SpanStyle(color = MaterialTheme.melon.status.bad)) { append(heart) }
                append(credit.substring(heartIndex + heart.length))
            } else {
                append(credit)
            }
        },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
        color = MaterialTheme.colorScheme.outlineVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
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
