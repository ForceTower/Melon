package dev.forcetower.unes.ui.feature.calendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.forcetower.unes.designsystem.theme.melon

// Visual tokens for each category, routed through the theme — never hex.
// The dc `CalendarScreen` category map lands exactly on existing adaptive
// tokens:
//   - Deadline → status.bad    (#D64A3C light / lifted red dark)
//   - Exam     → palette.violet (#7C4DD6 light / #B08CF0 dark)
//   - Holiday  → status.warn   (#C77F16 light / brand amber dark)
@Composable
@ReadOnlyComposable
internal fun CalendarCategory.color(): Color = when (this) {
    CalendarCategory.Holiday -> MaterialTheme.melon.status.warn
    CalendarCategory.Exam -> MaterialTheme.melon.palette.violet
    CalendarCategory.Deadline -> MaterialTheme.melon.status.bad
}

// Filled glyph rendered inside the category tiles (agenda rows, hero chip,
// event sheet) — mirrors the dc Material Symbols picks.
internal fun CalendarCategory.icon(): ImageVector = when (this) {
    CalendarCategory.Holiday -> Icons.Filled.WbSunny
    CalendarCategory.Exam -> Icons.Filled.Description
    CalendarCategory.Deadline -> Icons.Filled.Schedule
}
