package dev.forcetower.unes.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon

// Compact full-width segmented control — tonal track with a raised card
// thumb, mirroring the dc segmented rows (Calendário categories, campus-event
// audience filter). Deliberately NOT the M3 SegmentedButton: the design wants
// the iOS-style track/thumb look, `surfaceContainerHigh` so the track reads
// against the page in both themes, and a ~35dp height (plain clickable rows
// skip the 48dp minimum-touch-target inflation `Surface(onClick)` applies —
// the segments stay comfortably tappable at full width).
@Composable
fun <T> MelonSegmentedRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    // Optional leading tint dot per option; return null to omit.
    dot: ((option: T, active: Boolean) -> Color?)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            val shape = RoundedCornerShape(11.dp)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(if (active) Modifier.shadow(1.dp, shape) else Modifier)
                    .clip(shape)
                    .background(if (active) MaterialTheme.melon.surface.card else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                val tone = dot?.invoke(option, active)
                if (tone != null) {
                    Box(
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(tone),
                    )
                }
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.12).sp,
                    ),
                    color = if (active) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }
        }
    }
}
