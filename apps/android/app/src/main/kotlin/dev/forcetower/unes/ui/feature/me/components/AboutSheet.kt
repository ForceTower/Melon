package dev.forcetower.unes.ui.feature.me.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
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
            channel = if (debuggable) "desenvolvimento" else "estável",
            installSource = if (debuggable) "Debug" else "Play Store",
        )
    }
}

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
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
            Header(onClose = onDismiss)
            Spacer(Modifier.height(18.dp))
            Wordmark(info = info)
            Spacer(Modifier.height(12.dp))
            DetailRows(info = info)
            Spacer(Modifier.height(16.dp))
            CopyButton(
                copied = copied,
                onCopy = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("unes-debug", info.debugText))
                    copied = true
                },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.me_about_footer).uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.26.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Header(onClose: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink2 = MaterialTheme.colorScheme.onSurface
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val brand = MaterialTheme.melon.brand
    val variant = MaterialTheme.colorScheme.surfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = brand.plum.copy(alpha = 0.35f),
                    spotColor = brand.plum.copy(alpha = 0.35f),
                )
                .clip(RoundedCornerShape(12.dp))
                .background(brand.plum),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = brand.peach,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.me_about_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.33).sp,
                ),
                color = ink,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.me_about_subtitle).uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink3,
            )
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(variant)
                .clickable(role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            MeCloseGlyph(color = ink2, modifier = Modifier.size(11.dp))
        }
    }
}

@Composable
private fun Wordmark(info: AppInfo) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = accent, fontStyle = FontStyle.Italic)) {
                    append("UNES")
                }
            },
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 44.sp,
                lineHeight = 44.sp,
                letterSpacing = (-1.32).sp,
                fontWeight = FontWeight.Normal,
            ),
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = info.version,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.48).sp,
                ),
                color = ink,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.me_about_build_format, info.build).uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    letterSpacing = 0.95.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink4,
            )
        }
    }
}

@Composable
private fun DetailRows(info: AppInfo) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val line = MaterialTheme.melon.surface.line

    val rows = listOf(
        DetailRow(
            label = stringResource(R.string.me_about_row_version),
            value = info.version,
            sub = "${info.channel} · ${stringResource(R.string.me_about_row_version_origin)}",
            mono = false,
        ),
        DetailRow(
            label = stringResource(R.string.me_about_row_build),
            value = info.build,
            sub = stringResource(R.string.me_about_row_build_origin_format, info.installSource),
            mono = false,
        ),
        DetailRow(
            label = stringResource(R.string.me_about_row_machine_id),
            value = info.machineId,
            sub = stringResource(R.string.me_about_row_machine_id_sub),
            mono = true,
        ),
        DetailRow(
            label = stringResource(R.string.me_about_row_device),
            value = info.phoneModel,
            sub = stringResource(R.string.me_about_row_device_sub),
            mono = false,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(18.dp)),
    ) {
        rows.forEachIndexed { i, row ->
            DetailRowBody(row)
            if (i < rows.size - 1) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(line))
            }
        }
    }
}

@Composable
private fun DetailRowBody(row: DetailRow) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = row.label.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.26.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = ink4,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = row.value,
            style = if (row.mono) {
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.25.sp,
                )
            } else {
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.07).sp,
                )
            },
            color = ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = row.sub,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                letterSpacing = 0.4.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = ink4,
        )
    }
}

@Composable
private fun CopyButton(copied: Boolean, onCopy: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val surface = MaterialTheme.colorScheme.surface
    val bgTarget = if (copied) MaterialTheme.melon.fixed.ok else ink
    val fgTarget = if (copied) MaterialTheme.melon.fixed.surfaceLight else surface
    val bg by animateColorAsState(targetValue = bgTarget, label = "about-copy-bg")
    val fg by animateColorAsState(targetValue = fgTarget, label = "about-copy-fg")
    val label = stringResource(
        if (copied) R.string.me_about_copy_done else R.string.me_about_copy_label,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = bg.copy(alpha = 0.35f),
                spotColor = bg.copy(alpha = 0.35f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(role = Role.Button, onClick = onCopy, onClickLabel = label)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (copied) {
            MeCheckGlyph(color = fg, modifier = Modifier.size(13.dp))
        } else {
            MeCopyGlyph(color = fg, modifier = Modifier.size(13.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.07).sp,
            ),
            color = fg,
        )
    }
}

private data class DetailRow(
    val label: String,
    val value: String,
    val sub: String,
    val mono: Boolean,
)
