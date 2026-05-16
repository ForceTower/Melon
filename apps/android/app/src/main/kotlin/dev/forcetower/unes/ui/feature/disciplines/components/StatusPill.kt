package dev.forcetower.unes.ui.feature.disciplines.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.DisciplineScoreColor
import dev.forcetower.unes.ui.feature.disciplines.DisciplineStatus
import java.util.Locale

// Tiny uppercase pill showing the discipline's current status (aprovado / em
// andamento / nota baixa / etc). Mirrors iOS `StatusPill`.
@Composable
internal fun StatusPill(status: DisciplineStatus, modifier: Modifier = Modifier) {
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val surface2 = MaterialTheme.colorScheme.surfaceVariant

    val (bg, fg) = when (status.key) {
        DisciplineStatus.Key.Approved -> {
            val teal = DisciplineScoreColor.excellent()
            teal.copy(alpha = 0.13f) to teal.darken(0.32f)
        }
        DisciplineStatus.Key.Ongoing -> surface2 to ink3
        DisciplineStatus.Key.Low -> {
            val coral = DisciplineScoreColor.danger()
            coral.copy(alpha = 0.13f) to coral.darken(0.32f)
        }
        DisciplineStatus.Key.Failed -> {
            val coral = DisciplineScoreColor.danger()
            coral.copy(alpha = 0.20f) to coral.darken(0.32f)
        }
        DisciplineStatus.Key.Final -> {
            val amber = DisciplineScoreColor.caution()
            amber.copy(alpha = 0.13f) to amber.darken(0.40f)
        }
        DisciplineStatus.Key.Pending -> surface2 to ink4
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = status.label.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.9.sp,
            ),
            color = fg,
        )
    }
}

// Mix toward black by `amount` (0f = no change, 1f = pure black). Used to land
// readable pill foregrounds on the lifted dark-mode palette tones.
private fun Color.darken(amount: Float): Color = Color(
    red = (red * (1f - amount)).coerceIn(0f, 1f),
    green = (green * (1f - amount)).coerceIn(0f, 1f),
    blue = (blue * (1f - amount)).coerceIn(0f, 1f),
    alpha = alpha,
)
