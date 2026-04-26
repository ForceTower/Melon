package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Eyebrow-style section divider used between the hero card and the shortcut
// grid / settings list. Optional trailing action mirrors the JSX prototype's
// "gerenciar →" affordance. Kept inside the Me feature; the Overview screen
// uses its own header so this stays scoped.
@Composable
internal fun MeSectionLabel(
    label: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "◦ $label",
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.44.sp,
            ),
            color = ink3,
        )
        if (actionLabel != null) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.06).sp,
                    ),
                    color = ink3,
                )
                MeChevronGlyph(color = ink3, modifier = Modifier.size(9.dp))
            }
        }
    }
}
