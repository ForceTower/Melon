package dev.forcetower.unes.ui.feature.disciplines.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.DisciplineScoreColor
import kotlin.math.max

// Segmented bar showing absences used vs. total allowed. Color escalates from
// ink-3 → amber → coral as the ratio crosses 50% and 75%. Mirrors iOS
// `AbsenceBar` — same thresholds, same "{remaining} restantes" trailing label.
@Composable
internal fun AbsenceBar(
    used: Int,
    allowed: Int,
    modifier: Modifier = Modifier,
) {
    val ratio = if (allowed > 0) used.toDouble() / allowed.toDouble() else 0.0
    val tone = when {
        ratio >= 0.75 -> DisciplineScoreColor.danger()
        ratio >= 0.50 -> DisciplineScoreColor.caution()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val line = MaterialTheme.melon.surface.line
    val remaining = max(0, allowed - used)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        ) {
            repeat(allowed) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < used) tone else line.copy(alpha = 0.5f)),
                )
            }
            // Allowed == 0 still draws an empty bar so the row keeps height.
            if (allowed == 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(line.copy(alpha = 0.5f)),
                )
            }
        }
        Text(
            text = pluralStringResource(R.plurals.disciplines_absences_remaining, remaining, remaining),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = tone,
            maxLines = 1,
        )
    }
}
