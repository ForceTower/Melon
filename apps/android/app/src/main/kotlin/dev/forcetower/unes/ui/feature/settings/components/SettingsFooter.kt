package dev.forcetower.unes.ui.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R

// Version + posture stamp at the bottom of the Configurações scroll (dc
// `SettingsScreen` footer): "UNES vX · build N" over the reminder that sync
// cadence lives on the server.
@Composable
internal fun SettingsFooter(appVersion: String, appBuild: String, modifier: Modifier = Modifier) {
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val style = MaterialTheme.typography.bodySmall.copy(
        fontSize = 12.sp,
        lineHeight = 19.sp,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_footer_version_format, appVersion, appBuild),
            style = style,
            color = ink4,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.settings_footer_note),
            style = style,
            color = ink4,
            textAlign = TextAlign.Center,
        )
    }
}
