package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.MelonTheme

// Uppercase section eyebrow above the shortcut grid and the settings list —
// dc `EuScreen` "ATALHOS" / "DEFINIÇÕES" labels.
@Composable
internal fun MeSectionLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label.uppercase(LocalConfiguration.current.locales[0]),
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.44.sp,
        ),
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
    )
}

@Preview
@Composable
private fun MeSectionLabelPreview() {
    MelonTheme {
        MeSectionLabel(label = "Atalhos")
    }
}
