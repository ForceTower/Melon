package dev.forcetower.unes.ui.feature.calendar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.melon

// Visual tokens for each category, routed through the theme — never hex.
//   - Holiday  → brand.amber  (always-amber, matches the JSX `#F4A23C`)
//   - Deadline → brand.coral  (always-coral, matches `#E85D4E`)
//   - Exam     → palette.plum (adaptive: deep plum on light, lavender on dark
//     so plum-on-plum doesn't disappear in dark mode — same trick iOS uses)
@Composable
@ReadOnlyComposable
internal fun CalendarCategory.color(): Color = when (this) {
    CalendarCategory.Holiday -> MaterialTheme.melon.brand.amber
    CalendarCategory.Deadline -> MaterialTheme.melon.brand.coral
    CalendarCategory.Exam -> MaterialTheme.melon.palette.plum
}

// Mesh palette that fills the hero card backdrop. Same mapping iOS uses on
// `CalHeroCard`.
internal fun CalendarCategory.meshVariant(): MeshVariant = when (this) {
    CalendarCategory.Holiday -> MeshVariant.Warm
    CalendarCategory.Exam -> MeshVariant.Cool
    CalendarCategory.Deadline -> MeshVariant.Rose
}
