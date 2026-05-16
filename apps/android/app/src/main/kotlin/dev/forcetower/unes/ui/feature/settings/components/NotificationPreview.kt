package dev.forcetower.unes.ui.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.settings.SpoilerMode

// Lock-screen mock that reflects the current spoiler setting. Rendered above
// the spoiler picker so the student previews exactly what a new-grade alert
// looks like before flipping the knob. Mirrors `NotificationPreview.swift`.
@Composable
internal fun NotificationPreview(spoiler: SpoilerMode, modifier: Modifier = Modifier) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    // The mock notification stays on the always-dark surface so it reads as
    // a phone lock-screen across both themes — same treatment iOS uses.
    val mockBg = MaterialTheme.melon.brand.alwaysDarkBg
    val mockText = MaterialTheme.melon.fixed.surfaceLight
    val amber = MaterialTheme.melon.brand.amber
    val plum = MaterialTheme.melon.brand.plum

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_preview_eyebrow),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.08.sp,
            ),
            color = ink4,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(mockBg)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(amber),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.settings_preview_app_initial),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = plum,
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.settings_preview_app_name),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.05).sp,
                        ),
                        color = mockText,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.settings_preview_now),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            letterSpacing = 0.72.sp,
                        ),
                        color = mockText.copy(alpha = 0.55f),
                    )
                }
                Text(
                    text = stringResource(spoiler.previewBodyRes()),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        letterSpacing = (-0.05).sp,
                    ),
                    color = mockText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun SpoilerMode.previewBodyRes(): Int = when (this) {
    SpoilerMode.Value -> R.string.settings_preview_text_value
    SpoilerMode.Comment -> R.string.settings_preview_text_comment
    SpoilerMode.Posted -> R.string.settings_preview_text_posted
}
