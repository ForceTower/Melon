package dev.forcetower.unes.ui.feature.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.MelonGhostButton
import dev.forcetower.unes.designsystem.components.MelonPrimaryButton
import dev.forcetower.unes.designsystem.theme.melon

/**
 * Explains why Home stopped syncing while the network re-signs the API's TLS
 * (campus Fortinet, antivirus HTTPS inspection). Nothing here can fix it; the
 * retry only checks whether the student already moved to another network.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InterceptionSheet(
    issuerName: String?,
    isRetrying: Boolean,
    stillBlocked: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile()
                Spacer(Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.interception_sheet_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        letterSpacing = (-0.33).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (issuerName != null) {
                    stringResource(R.string.interception_sheet_description_named, issuerName)
                } else {
                    stringResource(R.string.interception_sheet_description_unnamed)
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.interception_sheet_hint),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (stillBlocked) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.interception_sheet_still_blocked),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.melon.status.bad,
                )
            }

            Spacer(Modifier.height(20.dp))

            MelonPrimaryButton(
                text = stringResource(R.string.interception_sheet_retry),
                onClick = onRetry,
                enabled = !isRetrying,
                isLoading = isRetrying,
            )

            Spacer(Modifier.height(10.dp))

            MelonGhostButton(
                text = stringResource(R.string.interception_sheet_dismiss),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun IconTile() {
    val warn = MaterialTheme.melon.status.warn
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(warn.copy(alpha = 0.12f))
            .border(1.dp, warn.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.GppMaybe,
            contentDescription = null,
            tint = warn,
            modifier = Modifier.size(18.dp),
        )
    }
}
