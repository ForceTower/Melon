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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.me.ProfileIdentity
import java.util.Locale

// Confirmation sheet rendered when the user taps "Sair da conta". Mirrors
// `LogoutConfirmationSheet` on iOS — destructive icon header, account
// summary card, copy explaining what happens, and a keep-data toggle. The
// bottom row hosts a balanced cancel/destructive pair where the destructive
// half is wider (1.2× weight) so the eye lands on the verb.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogoutSheet(
    identity: ProfileIdentity,
    onCancel: () -> Unit,
    onConfirm: (keepData: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var keepData by rememberSaveable { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Header(identity = identity)
            Spacer(Modifier.height(14.dp))
            AccountCard(identity = identity)
            Spacer(Modifier.height(12.dp))
            Description()
            Spacer(Modifier.height(14.dp))
            KeepDataToggle(checked = keepData, onToggle = { keepData = !keepData })
            Spacer(Modifier.height(14.dp))
            FooterRow(onCancel = onCancel, onConfirm = { onConfirm(keepData) })
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Header(identity: ProfileIdentity) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val destructive = MaterialTheme.melon.fixed.destructive
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(destructive.copy(alpha = 0.12f))
                .border(1.dp, destructive.copy(alpha = 0.25f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            MeExitGlyph(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.me_logout_sheet_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.33).sp,
                ),
                color = ink,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.me_logout_sheet_eyebrow_format, identity.enrollment)
                    .uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink3,
            )
        }
    }
}

@Composable
private fun AccountCard(identity: ProfileIdentity) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val brand = MaterialTheme.melon.brand
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
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
                text = identity.avatarInitial,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = MaterialTheme.melon.fixed.surfaceLight,
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = identity.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = ink,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.me_logout_sheet_account_username_format,
                    identity.username.ifBlank { "—" },
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    letterSpacing = 0.57.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink4,
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
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

@Composable
private fun KeepDataToggle(checked: Boolean, onToggle: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val variant = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.melon.surface.line
    val accept = MaterialTheme.melon.fixed.ok
    val onAccept = MaterialTheme.melon.fixed.surfaceLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(variant)
            .border(1.dp, line, RoundedCornerShape(14.dp))
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) accept else Color.Transparent)
                .border(1.5.dp, if (checked) accept else ink4, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                MeCheckGlyph(color = onAccept, modifier = Modifier.size(11.dp))
            }
        }
        Spacer(Modifier.size(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.me_logout_sheet_keep_data_label),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.me_logout_sheet_keep_data_hint),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.54.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink4,
            )
        }
    }
}

@Composable
private fun FooterRow(onCancel: () -> Unit, onConfirm: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val variant = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.melon.surface.line
    val destructive = MaterialTheme.melon.fixed.destructive
    val onDestructive = MaterialTheme.melon.fixed.surfaceLight
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Cancel — neutral surface pill.
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(variant)
                .border(1.dp, line, RoundedCornerShape(16.dp))
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.me_logout_sheet_cancel),
                    onClick = onCancel,
                )
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.me_logout_sheet_cancel),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.07).sp,
                ),
                color = ink,
            )
        }
        // Confirm — destructive fill, slightly wider (1.2× weight) so the
        // verb is the visual anchor. Drop shadow tinted with the same color
        // sells the affordance.
        Row(
            modifier = Modifier
                .weight(1.2f)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = destructive.copy(alpha = 0.35f),
                    spotColor = destructive.copy(alpha = 0.35f),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(destructive)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.me_logout_sheet_confirm),
                    onClick = onConfirm,
                )
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        ) {
            MeExitGlyph(color = onDestructive, modifier = Modifier.size(14.dp))
            Text(
                text = stringResource(R.string.me_logout_sheet_confirm),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.07).sp,
                ),
                color = onDestructive,
            )
        }
    }
}
