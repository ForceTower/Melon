package dev.forcetower.unes.ui.feature.me.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.melon
import java.util.Locale
import kotlinx.coroutines.delay

// Snapshot of the live build/device metadata surfaced by the sheet. Reads
// happen at composition time so the values are always live (no fixture path
// — mirrors iOS `AppInfo.current`).
internal data class AppInfo(
    val version: String,
    val build: String,
    val machineId: String,
    val phoneModel: String,
    val deviceName: String,
    val osVersion: String,
    val channel: String,
    val installSource: String,
) {
    val debugText: String =
        """
        UNES — debug info
        versão     $version
        build      $build
        machine id $machineId
        aparelho   $phoneModel
        """.trimIndent()
}

@Composable
internal fun rememberAppInfo(): AppInfo {
    val context = LocalContext.current
    return remember(context) {
        val pm = context.packageManager
        val info = pm.getPackageInfo(context.packageName, 0)
        val versionName = info.versionName.orEmpty().ifBlank { "—" }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION") info.versionCode.toString()
        }
        // `ApplicationInfo.FLAG_DEBUGGABLE` is the runtime-equivalent of
        // `BuildConfig.DEBUG` and works without leaking the app module's
        // generated BuildConfig type into this composable's caller.
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        AppInfo(
            version = versionName,
            build = versionCode,
            // ANDROID_ID is per-app on Android 8+ — close enough to iOS's
            // `identifierForVendor` for a debug-info field.
            machineId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID,
            ).orEmpty().lowercase(Locale.ROOT).ifBlank { "—" },
            phoneModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}",
            deviceName = Build.MODEL,
            osVersion = Build.VERSION.RELEASE.orEmpty().ifBlank { "—" },
            channel = if (debuggable) "desenvolvimento" else "estável",
            installSource = if (debuggable) "Debug" else "Play Store",
        )
    }
}

// "Sobre o aplicativo" M3 bottom sheet — dc `EuScreen` about sheet: header
// with the indigo info tile, the always-dark app hero (icon tile, wordmark,
// tagline, version), the 2×2 info grid, and the copy-debug-info pill.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val info = rememberAppInfo()
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    if (copied) {
        LaunchedEffect(copied) {
            delay(1800)
            copied = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 26.dp)) {
            Header(onClose = onDismiss)
            Spacer(Modifier.height(22.dp))
            AppHero(info = info)
            Spacer(Modifier.height(14.dp))
            InfoGrid(info = info)
            Spacer(Modifier.height(16.dp))
            CopyButton(
                copied = copied,
                onCopy = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("unes-debug", info.debugText))
                    copied = true
                },
            )
            Spacer(Modifier.height(16.dp))
            Footer()
        }
    }
}

@Composable
private fun Header(onClose: () -> Unit) {
    val indigo = MaterialTheme.melon.palette.indigo
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(indigo.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = indigo,
                modifier = Modifier.size(26.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(top = 1.dp)) {
            Text(
                text = stringResource(R.string.me_about_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 21.sp,
                    lineHeight = 23.sp,
                    letterSpacing = (-0.42).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.me_about_subtitle),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.me_document_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun AppHero(info: AppInfo) {
    val fixed = MaterialTheme.melon.fixed
    val brand = MaterialTheme.melon.brand
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = shape)
            .clip(shape)
            .background(fixed.heroNight),
    ) {
        Mesh(variant = MeshVariant.Rose, intensity = 0.7f, modifier = Modifier.matchParentSize())
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = fixed.heroVeil,
                        spotColor = fixed.heroVeil,
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            0f to brand.amber,
                            0.55f to brand.coral,
                            1f to brand.magenta,
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.me_about_hero_initial),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = fixed.onHero,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.me_about_hero_wordmark),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = fixed.onHero,
                    maxLines = 1,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = stringResource(R.string.me_about_hero_tagline),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = fixed.onHero.copy(alpha = 0.82f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = info.version,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.48).sp,
                ),
                color = fixed.onHero,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Long dev version names must never squeeze the wordmark —
                // release versions ("0.13.0") fit comfortably under the cap.
                modifier = Modifier.widthIn(max = 132.dp),
            )
        }
    }
}

@Composable
private fun InfoGrid(info: AppInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoTile(
                label = stringResource(R.string.me_about_tile_build),
                value = info.build,
                sub = stringResource(R.string.me_about_tile_build_sub_format, info.installSource),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            InfoTile(
                label = stringResource(R.string.me_about_tile_channel),
                value = info.channel,
                sub = stringResource(R.string.me_about_tile_channel_sub_format, info.version),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoTile(
                label = stringResource(R.string.me_about_tile_device),
                value = info.deviceName,
                sub = stringResource(R.string.me_about_tile_device_sub_format, info.osVersion),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            InfoTile(
                label = stringResource(R.string.me_about_tile_machine_id),
                value = info.machineId,
                sub = stringResource(R.string.me_about_tile_machine_id_sub),
                mono = true,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun InfoTile(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                letterSpacing = 0.66.sp,
            ),
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = if (mono) FontFamily.Monospace else null,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = sub,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CopyButton(copied: Boolean, onCopy: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val label = stringResource(
        if (copied) R.string.me_about_copy_done else R.string.me_about_copy_label,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .background(accent)
            .clickable(role = Role.Button, onClick = onCopy, onClickLabel = label),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
            contentDescription = null,
            tint = onAccent,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            color = onAccent,
        )
    }
}

@Composable
private fun Footer() {
    val credit = stringResource(R.string.me_footer_credit)
    val heart = "♥"
    Text(
        text = buildAnnotatedString {
            val heartIndex = credit.indexOf(heart)
            if (heartIndex >= 0) {
                append(credit.substring(0, heartIndex))
                withStyle(SpanStyle(color = MaterialTheme.melon.status.bad)) { append(heart) }
                append(credit.substring(heartIndex + heart.length))
            } else {
                append(credit)
            }
        },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
        color = MaterialTheme.colorScheme.outlineVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
