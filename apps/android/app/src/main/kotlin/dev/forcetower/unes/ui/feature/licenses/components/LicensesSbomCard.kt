package dev.forcetower.unes.ui.feature.licenses.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.licenses.LicensesAssetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// "Baixar manifesto de licenças" — exports the bundled `artifacts.json` (the
// Licensee manifest that drives this whole screen) through the system
// "Salvar em…" picker, so the user gets the exact JSON that shipped in the APK.
// Uses the Storage Access Framework, so there's no runtime permission and no
// FileProvider to wire.
@Composable
internal fun LicensesSbomCard(
    sizeBytes: Int,
    modifier: Modifier = Modifier,
) {
    // The SAF launcher needs an ActivityResultRegistryOwner, which previews
    // don't provide — guard on inspection mode so the card still renders there.
    val onExport: () -> Unit = if (LocalInspectionMode.current) {
        {}
    } else {
        rememberManifestExport()
    }

    val card = MaterialTheme.melon.surface.card
    val line = MaterialTheme.melon.surface.line
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(card)
            .border(1.dp, line, RoundedCornerShape(22.dp))
            .clickable(onClick = onExport)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.licenses_sbom_title),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = ink,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.licenses_sbom_meta_format, kilobytes(sizeBytes)),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                ),
                color = ink3,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = ink4,
            modifier = Modifier.size(22.dp),
        )
    }
}

// Wires the SAF create-document launcher and returns the tap handler. Kept
// separate so the card can skip it entirely under `LocalInspectionMode`.
@Composable
private fun rememberManifestExport(): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val filename = stringResource(R.string.licenses_sbom_filename)
    val saver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val bytes = LicensesAssetLoader.rawManifest(context) ?: return@withContext
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    }
                }
            }
        }
    }
    return { saver.launch(filename) }
}

// Whole-KB rounding for the "Licensee · JSON · N KB" meta, floored at 1 so a
// tiny manifest never reads "0 KB".
private fun kilobytes(bytes: Int): Int = ((bytes + 512) / 1024).coerceAtLeast(1)
