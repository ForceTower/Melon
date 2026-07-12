package dev.forcetower.unes.ui.feature.materials.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon

// Transient confirmation pill (dc toast): inverse-surface plate, tinted glyph,
// one line of copy. The detail VM owns the 2.2s lifetime.
internal enum class MaterialsToastKind {
    Saved,
    Unsaved,
    Reported,
    SyncFailed,
    OpenFailed,
}

@Composable
internal fun MaterialsToastOverlay(
    toast: MaterialsToastKind?,
    modifier: Modifier = Modifier,
) {
    // Freeze the last non-null kind so the exit animation keeps its copy.
    var lastKind by remember { mutableStateOf<MaterialsToastKind?>(null) }
    if (toast != null) lastKind = toast
    AnimatedVisibility(
        visible = toast != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        val kind = lastKind ?: return@AnimatedVisibility
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(horizontal = 16.dp, vertical = 13.dp),
        ) {
            Icon(
                imageVector = kind.icon(),
                contentDescription = null,
                tint = kind.tint(),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(kind.textRes()),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

private fun MaterialsToastKind.icon(): ImageVector = when (this) {
    MaterialsToastKind.Saved -> Icons.Filled.Bookmark
    MaterialsToastKind.Unsaved -> Icons.Filled.BookmarkRemove
    MaterialsToastKind.Reported -> Icons.Filled.VerifiedUser
    MaterialsToastKind.SyncFailed -> Icons.Filled.CloudOff
    MaterialsToastKind.OpenFailed -> Icons.Filled.ErrorOutline
}

@Composable
private fun MaterialsToastKind.tint(): Color = when (this) {
    MaterialsToastKind.Saved, MaterialsToastKind.Unsaved -> MaterialTheme.melon.status.ok
    MaterialsToastKind.Reported -> MaterialTheme.melon.status.bad
    MaterialsToastKind.SyncFailed, MaterialsToastKind.OpenFailed -> MaterialTheme.melon.status.warn
}

private fun MaterialsToastKind.textRes(): Int = when (this) {
    MaterialsToastKind.Saved -> R.string.materials_toast_saved
    MaterialsToastKind.Unsaved -> R.string.materials_toast_unsaved
    MaterialsToastKind.Reported -> R.string.materials_toast_reported
    MaterialsToastKind.SyncFailed -> R.string.materials_toast_sync_failed
    MaterialsToastKind.OpenFailed -> R.string.materials_toast_open_failed
}
