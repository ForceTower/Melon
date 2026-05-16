package dev.forcetower.unes.ui.feature.disciplines

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.forcetower.unes.designsystem.theme.melon

// Score ramp used across the Disciplinas screen: teal for excellent, ink for
// good, amber for passing-but-shaky, coral for below the cutoff. Mirrors iOS
// `DisciplineScoreColor` — same thresholds. Adaptive: pulls from the
// MelonPalette so the ramp lifts on dark mode the same way every other tinted
// surface does.
internal object DisciplineScoreColor {
    @Composable
    @ReadOnlyComposable
    fun excellent(): Color = MaterialTheme.melon.palette.teal

    @Composable
    @ReadOnlyComposable
    fun caution(): Color = MaterialTheme.melon.palette.amber

    @Composable
    @ReadOnlyComposable
    fun danger(): Color = MaterialTheme.melon.palette.coral

    @Composable
    @ReadOnlyComposable
    fun colorFor(score: Double?): Color {
        if (score == null) return MaterialTheme.colorScheme.outlineVariant
        return when {
            score >= 8.5 -> excellent()
            score >= 7.0 -> MaterialTheme.colorScheme.onBackground
            score >= 5.0 -> caution()
            else -> danger()
        }
    }
}
