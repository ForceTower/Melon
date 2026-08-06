package dev.forcetower.unes.ui.feature.me.components

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
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.me.MeProfileToast

// Transient confirmation pill after a profile-customization save — dc
// `EuScreen` snackbar, same inverse-surface treatment as the materials toast.
// The ViewModel owns the 2.6s lifetime.
@Composable
internal fun MeProfileToastOverlay(
    toast: MeProfileToast?,
    modifier: Modifier = Modifier,
) {
    // Freeze the last non-null toast so the exit animation keeps its copy.
    var lastToast by remember { mutableStateOf<MeProfileToast?>(null) }
    if (toast != null) lastToast = toast
    AnimatedVisibility(
        visible = toast != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        val shown = lastToast ?: return@AnimatedVisibility
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(horizontal = 16.dp, vertical = 13.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.melon.status.ok,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = shown.message(),
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

@Composable
private fun MeProfileToast.message(): String = when (this) {
    is MeProfileToast.NameSaved -> stringResource(R.string.me_toast_name_saved_format, firstName)
    MeProfileToast.NameRestored -> stringResource(R.string.me_toast_name_restored)
    MeProfileToast.PhotoSaved -> stringResource(R.string.me_toast_photo_saved)
    MeProfileToast.PhotoRemoved -> stringResource(R.string.me_toast_photo_removed)
}
