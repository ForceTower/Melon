package dev.forcetower.unes.ui.feature.onboarding.login

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.MelonGhostButton
import dev.forcetower.unes.designsystem.components.MelonPrimaryButton
import dev.forcetower.unes.designsystem.theme.melon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val portalUrl = stringResource(R.string.onboarding_forgot_password_portal_url)

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
                Column {
                    Text(
                        text = stringResource(R.string.onboarding_forgot_password_label),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.onboarding_forgot_password_title),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 22.sp,
                            letterSpacing = (-0.33).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.onboarding_forgot_password_description),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            Step(number = 1, text = stringResource(R.string.onboarding_forgot_password_step_1))
            Step(number = 2, text = stringResource(R.string.onboarding_forgot_password_step_2))
            Step(number = 3, text = stringResource(R.string.onboarding_forgot_password_step_3))

            Spacer(Modifier.height(24.dp))

            MelonPrimaryButton(
                text = stringResource(R.string.onboarding_forgot_password_open_portal),
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, portalUrl.toUri())
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    onDismiss()
                },
            )

            Spacer(Modifier.height(10.dp))

            MelonGhostButton(
                text = stringResource(R.string.onboarding_forgot_password_back),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun IconTile() {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            ),
            color = MaterialTheme.melon.brand.amber,
            modifier = Modifier
                .padding(end = 12.dp, top = 2.dp)
                .size(width = 16.dp, height = 16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
