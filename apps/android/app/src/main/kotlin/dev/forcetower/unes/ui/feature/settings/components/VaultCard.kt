package dev.forcetower.unes.ui.feature.settings.components

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.theme.melon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Credential vault — the always-dark green mesh card at the top of
// Configurações (dc `SettingsScreen` credenciais block). Identity row with
// the brand-gradient avatar, a Ver/Ocultar pill, and the Usuário/Senha rows
// with copy buttons. Revealing the password demands a device-owner
// authentication prompt (biometric with screen-lock fallback) so a borrowed
// phone can't pop it by tapping; copying it additionally requires it to be
// visible.
@Composable
internal fun VaultCard(
    displayName: String,
    accountLabel: String,
    avatarInitial: String,
    username: String,
    password: String,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    passkeyCount: Int?,
    onOpenPasskeys: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vault = MaterialTheme.melon.vault
    val shape = RoundedCornerShape(28.dp)

    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    val promptTitle = stringResource(R.string.settings_credentials_biometric_prompt_title)
    val promptSubtitle = stringResource(R.string.settings_credentials_biometric_prompt_subtitle)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, spotColor = vault.night, ambientColor = vault.night)
            .clip(shape)
            .background(vault.night),
    ) {
        Mesh(colors = vault.blobs, modifier = Modifier.matchParentSize())
        // Legibility scrim: dim top and bottom, breathe in the middle —
        // matches the dc `linear-gradient(180deg, .30 / .08 45% / .40)` veil.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to vault.veil.copy(alpha = 0.30f),
                        0.45f to vault.veil.copy(alpha = 0.08f),
                        1f to vault.veil.copy(alpha = 0.40f),
                    ),
                ),
        )

        Column(modifier = Modifier.padding(20.dp)) {
            VaultHeader()
            Spacer(Modifier.height(18.dp))
            IdentityRow(
                displayName = displayName,
                accountLabel = accountLabel,
                avatarInitial = avatarInitial,
                revealed = revealed,
                onToggleReveal = {
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
            Spacer(Modifier.height(18.dp))
            FieldsBox(username = username, password = password, revealed = revealed)
            Spacer(Modifier.height(14.dp))
            PasskeysRow(count = passkeyCount, onClick = onOpenPasskeys)
        }
    }
}

// Entry row to the passkeys manager, sitting under the credential fields on the
// always-dark vault card. The "N ativa(s)" pill only shows once the count has
// resolved to a non-zero value.
@Composable
private fun PasskeysRow(count: Int?, onClick: () -> Unit) {
    val onHero = MaterialTheme.melon.fixed.onHero
    val live = MaterialTheme.melon.fixed.live
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(onHero.copy(alpha = 0.07f))
            .border(1.dp, onHero.copy(alpha = 0.12f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(onHero.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Key,
                contentDescription = null,
                tint = onHero,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_passkeys_row_title),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = onHero,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.settings_passkeys_row_subtitle),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = onHero.copy(alpha = 0.7f),
            )
        }
        if (count != null && count > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(live.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = pluralStringResource(R.plurals.settings_passkeys_active_count, count, count),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = live,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = onHero.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun VaultHeader() {
    val onHero = MaterialTheme.melon.fixed.onHero
    val live = MaterialTheme.melon.fixed.live

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(onHero.copy(alpha = 0.12f))
                .padding(start = 10.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(live),
            )
            Text(
                text = stringResource(R.string.settings_vault_pill).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.54.sp,
                ),
                color = onHero,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Fingerprint,
                contentDescription = null,
                tint = onHero.copy(alpha = 0.82f),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.settings_vault_badge),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                ),
                color = onHero.copy(alpha = 0.82f),
            )
        }
    }
}

@Composable
private fun IdentityRow(
    displayName: String,
    accountLabel: String,
    avatarInitial: String,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
) {
    val brand = MaterialTheme.melon.brand
    val vault = MaterialTheme.melon.vault
    val onHero = MaterialTheme.melon.fixed.onHero

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(4.dp, CircleShape, spotColor = vault.veil, ambientColor = vault.veil)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        0f to brand.amber,
                        0.52f to brand.coral,
                        1f to brand.magenta,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = avatarInitial,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = onHero,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 19.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.38).sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = onHero,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = accountLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = onHero.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(onHero.copy(alpha = 0.14f))
                .clickable(role = Role.Button, onClick = onToggleReveal)
                .padding(horizontal = 15.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = null,
                tint = onHero,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = stringResource(
                    if (revealed) R.string.settings_vault_action_hide
                    else R.string.settings_vault_action_show,
                ),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                ),
                color = onHero,
            )
        }
    }
}

@Composable
private fun FieldsBox(username: String, password: String, revealed: Boolean) {
    val onHero = MaterialTheme.melon.fixed.onHero
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(onHero.copy(alpha = 0.07f))
            .border(1.dp, onHero.copy(alpha = 0.12f), shape),
    ) {
        FieldRow(
            label = stringResource(R.string.settings_vault_field_username),
            value = username,
            copyValue = username,
            canCopy = username.isNotEmpty(),
            sensitive = false,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(onHero.copy(alpha = 0.10f)),
        )
        FieldRow(
            label = stringResource(R.string.settings_vault_field_password),
            value = if (revealed) password else MaskedPassword,
            copyValue = password,
            canCopy = revealed && password.isNotEmpty(),
            sensitive = true,
        )
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    copyValue: String,
    canCopy: Boolean,
    sensitive: Boolean,
) {
    val onHero = MaterialTheme.melon.fixed.onHero
    val live = MaterialTheme.melon.fixed.live

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    val copyDescription = stringResource(
        if (copied) R.string.settings_vault_copied else R.string.settings_vault_copy,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            ),
            color = onHero.copy(alpha = 0.6f),
            modifier = Modifier.width(62.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.45.sp,
            ),
            color = onHero,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = canCopy, role = Role.Button) {
                    copyToClipboard(context, label, copyValue, sensitive)
                    copied = true
                    scope.launch {
                        delay(CopiedFlashMs)
                        copied = false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                contentDescription = copyDescription,
                tint = when {
                    copied -> live
                    canCopy -> onHero.copy(alpha = 0.75f)
                    else -> onHero.copy(alpha = 0.35f)
                },
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// Fixed-width mask — never leaks the real password length.
private const val MaskedPassword = "••••••••"
private const val CopiedFlashMs = 1400L

@SuppressLint("InlinedApi")
private fun copyToClipboard(context: Context, label: String, value: String, sensitive: Boolean) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val clip = ClipData.newPlainText(label, value)
    if (sensitive) {
        // Keeps Android 13+'s clipboard overlay from echoing the password on
        // screen and hints sync'd clipboards to skip it.
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    clipboard.setPrimaryClip(clip)
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
