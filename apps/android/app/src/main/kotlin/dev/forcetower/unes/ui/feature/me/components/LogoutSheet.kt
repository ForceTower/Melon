package dev.forcetower.unes.ui.feature.me.components

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
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.me.ProfileIdentity

// Confirmation sheet rendered when the user taps "Sair da conta" — dc
// `EuScreen` logout-confirm step. Error-tonal icon tile + "Sair da conta?"
// header with the session owner, one paragraph explaining that nothing stays
// on the device, and a cancel/destructive pill pair where the destructive
// half is slightly wider (1.25× weight) so the eye lands on the verb.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogoutSheet(
    identity: ProfileIdentity,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
        ) {
            Header(identity = identity)
            Spacer(Modifier.height(16.dp))
            Description()
            Spacer(Modifier.height(20.dp))
            FooterRow(onCancel = onCancel, onConfirm = onConfirm)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Header(identity: ProfileIdentity) {
    val err = MaterialTheme.melon.status.bad
    val shape = RoundedCornerShape(16.dp)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(shape)
                .background(err.copy(alpha = 0.12f).compositeOver(MaterialTheme.melon.surface.card))
                .border(1.dp, err.copy(alpha = 0.32f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = err,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.me_logout_sheet_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 21.sp,
                    lineHeight = 23.sp,
                    letterSpacing = (-0.42).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.me_logout_sheet_session_format, identity.name),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Description() {
    Text(
        text = stringResource(R.string.me_logout_sheet_description),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FooterRow(onCancel: () -> Unit, onConfirm: () -> Unit) {
    val err = MaterialTheme.melon.status.bad
    val onErr = MaterialTheme.melon.fixed.onHero
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Cancel — outlined ghost pill.
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(shape)
                .border(1.dp, MaterialTheme.melon.surface.line, shape)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.me_logout_sheet_cancel),
                    onClick = onCancel,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.me_logout_sheet_cancel),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        // Confirm — error fill, slightly wider (1.25× weight) so the verb is
        // the visual anchor. Drop shadow tinted with the same color sells the
        // affordance.
        Row(
            modifier = Modifier
                .weight(1.25f)
                .height(52.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = shape,
                    ambientColor = err.copy(alpha = 0.38f),
                    spotColor = err.copy(alpha = 0.38f),
                )
                .clip(shape)
                .background(err)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.me_logout_sheet_confirm),
                    onClick = onConfirm,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = onErr,
                modifier = Modifier.size(19.dp),
            )
            Text(
                text = stringResource(R.string.me_logout_sheet_confirm),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = onErr,
            )
        }
    }
}
