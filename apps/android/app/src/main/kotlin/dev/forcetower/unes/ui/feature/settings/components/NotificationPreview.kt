package dev.forcetower.unes.ui.feature.settings.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.settings.SpoilerMode

// Lock-screen mock that reflects the current privacy choice (dc
// `SettingsScreen` "Notas" preview). Rendered above the privacy segmented
// control so the student previews exactly what a new-grade alert reveals
// before flipping the knob. The plate is always dark so it reads as a phone
// lock screen across both themes.
@Composable
internal fun NotificationPreview(
    spoiler: SpoilerMode,
    clockLabel: String,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.melon.surface
    val onHero = MaterialTheme.melon.fixed.onHero

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(surface.previewPlate)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.settings_preview_context),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                ),
                color = onHero.copy(alpha = 0.72f),
            )
            Text(
                text = clockLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                ),
                color = onHero.copy(alpha = 0.72f),
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(surface.previewCard)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppBadge()
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_preview_app_name),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.44.sp,
                        ),
                        color = onHero,
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(onHero.copy(alpha = 0.6f)),
                    )
                    Text(
                        text = stringResource(R.string.settings_preview_now),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = onHero.copy(alpha = 0.7f),
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(spoiler.previewTitleRes()),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 17.sp,
                    ),
                    color = onHero,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(spoiler.previewBodyRes()),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                    ),
                    color = onHero.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AppBadge() {
    val brand = MaterialTheme.melon.brand
    val onHero = MaterialTheme.melon.fixed.onHero
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
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
            text = stringResource(R.string.settings_preview_app_initial),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            color = onHero,
        )
    }
}

private fun SpoilerMode.previewTitleRes(): Int = when (this) {
    SpoilerMode.Value -> R.string.settings_preview_value_title
    SpoilerMode.Comment -> R.string.settings_preview_summary_title
    SpoilerMode.Posted -> R.string.settings_preview_discreet_title
}

private fun SpoilerMode.previewBodyRes(): Int = when (this) {
    SpoilerMode.Value -> R.string.settings_preview_value_body
    SpoilerMode.Comment -> R.string.settings_preview_summary_body
    SpoilerMode.Posted -> R.string.settings_preview_discreet_body
}
