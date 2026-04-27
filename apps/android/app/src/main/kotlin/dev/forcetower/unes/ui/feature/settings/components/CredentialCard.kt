package dev.forcetower.unes.ui.feature.settings.components

import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.settings.SettingsTone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Credential vault card — header pill toggles reveal/hide; copy is gated on
// the password being visible. Reveal is gated behind a device-owner
// authentication prompt (biometric or screen-lock fallback) so a borrowed
// phone can't pop the password by tapping. Mirrors `CredentialCard.swift`.
@Composable
internal fun CredentialCard(
    username: String,
    password: String,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val surface2 = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.melon.surface.line
    val tone = resolveTone(SettingsTone.Plum)
    val masked = remember(password) { "•".repeat(password.length.coerceAtLeast(8)) }

    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    val promptTitle = stringResource(R.string.settings_credentials_biometric_prompt_title)
    val promptSubtitle = stringResource(R.string.settings_credentials_biometric_prompt_subtitle)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(
            tone = tone,
            revealed = revealed,
            onToggle = {
                if (revealed) {
                    onToggleReveal()
                } else {
                    requestBiometricReveal(
                        context = context,
                        activity = activity,
                        title = promptTitle,
                        subtitle = promptSubtitle,
                        onSuccess = onToggleReveal,
                    )
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(surface2)
                .border(1.dp, line, RoundedCornerShape(14.dp)),
        ) {
            CredentialField(
                label = stringResource(R.string.settings_credentials_field_username),
                value = username,
                canCopy = username.isNotEmpty(),
            )
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(line))
            CredentialField(
                label = stringResource(R.string.settings_credentials_field_password),
                value = if (revealed) password else masked,
                canCopy = revealed && password.isNotEmpty(),
            )
        }
    }
}

@Composable
private fun Header(
    tone: ResolvedSettingsTone,
    revealed: Boolean,
    onToggle: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val surface2 = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.melon.surface.line

    val pillBg = if (revealed) tone.background else surface2
    val pillBorder = if (revealed) tone.background else line
    val pillFg = if (revealed) tone.foreground else ink

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(tone.background),
            contentAlignment = Alignment.Center,
        ) {
            SettingsIcon(glyph = SettingsGlyph.Key, color = tone.foreground, modifier = Modifier.size(15.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_credentials_title),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.07).sp,
                ),
                color = ink,
            )
            Text(
                text = stringResource(
                    if (revealed) R.string.settings_credentials_status_visible
                    else R.string.settings_credentials_status_hidden
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.76.sp,
                ),
                color = ink4,
            )
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(pillBg)
                .border(1.dp, pillBorder, CircleShape)
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SettingsIcon(
                glyph = if (revealed) SettingsGlyph.EyeOff else SettingsGlyph.Eye,
                color = pillFg,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = stringResource(
                    if (revealed) R.string.settings_credentials_action_hide
                    else R.string.settings_credentials_action_show
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.06).sp,
                ),
                color = pillFg,
            )
        }
    }
}

@Composable
private fun CredentialField(label: String, value: String, canCopy: Boolean) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.08.sp,
            ),
            color = ink4,
            modifier = Modifier.size(width = 64.dp, height = 14.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                letterSpacing = 0.26.sp,
            ),
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(enabled = canCopy) {
                    copyToClipboard(context, label, value)
                    copied = true
                    scope.launch {
                        delay(1400L)
                        copied = false
                    }
                }
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (copied) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SettingsIcon(glyph = SettingsGlyph.Check, color = ink3, modifier = Modifier.size(12.dp))
                    Text(
                        text = stringResource(R.string.settings_credentials_copy_done),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            letterSpacing = 0.72.sp,
                        ),
                        color = ink3,
                    )
                }
            } else {
                SettingsIcon(
                    glyph = SettingsGlyph.Copy,
                    color = if (canCopy) ink3 else ink4.copy(alpha = 0.4f),
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}

// Hiding the password is free; revealing demands a live device-owner check so
// a borrowed phone can't expose it by tapping. Mirrors iOS'
// `LAPolicy.deviceOwnerAuthentication` — biometric primary with screen-lock
// fallback. If the device has no lock at all, we let the reveal through
// rather than strand the user (same fallback iOS takes when
// `canEvaluatePolicy` is false). Combined BIOMETRIC_STRONG | DEVICE_CREDENTIAL
// is only valid on API 30+; below that we use the deprecated
// `setDeviceCredentialAllowed(true)` which expresses the same intent.
private fun requestBiometricReveal(
    context: Context,
    activity: FragmentActivity?,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
) {
    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    if (activity == null || keyguard?.isDeviceSecure != true) {
        onSuccess()
        return
    }

    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
            } else {
                @Suppress("DEPRECATION")
                setDeviceCredentialAllowed(true)
            }
        }
        .build()

    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(context),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
    prompt.authenticate(info)
}
