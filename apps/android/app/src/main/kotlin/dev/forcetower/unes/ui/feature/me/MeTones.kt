package dev.forcetower.unes.ui.feature.me

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.forcetower.unes.designsystem.theme.melon

// Resolve a `ShortcutTone` to its palette hue. The tonal card derives every
// surface from this one color (8% plate, 20% border/icon container, full-hue
// icon), the same recipe `DisciplineCard` uses.
@Composable
@ReadOnlyComposable
internal fun ShortcutTone.hue(): Color {
    val palette = MaterialTheme.melon.palette
    return when (this) {
        ShortcutTone.Teal -> palette.teal
        ShortcutTone.Coral -> palette.coral
        ShortcutTone.Magenta -> palette.magenta
        ShortcutTone.Indigo -> palette.indigo
        ShortcutTone.Violet -> palette.violet
        ShortcutTone.Amber -> palette.amber
        ShortcutTone.Green -> palette.green
    }
}
