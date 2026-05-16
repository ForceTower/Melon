package dev.forcetower.unes.ui.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import java.util.Locale

// Signature row stamped at the bottom of the Settings scroll. Two accent dots
// flanking an italic "unes" wordmark, then the build/version line. Mirrors
// `SettingsFooter` on iOS and `CfgFooter` in the JSX prototype.
@Composable
internal fun SettingsFooter(appVersion: String, appBuild: String, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val ink2 = MaterialTheme.colorScheme.onSurface
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accent))
            Text(
                text = stringResource(R.string.settings_footer_wordmark),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-0.16).sp,
                ),
                color = ink2,
            )
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accent))
        }
        Text(
            text = stringResource(R.string.settings_footer_meta_format, appVersion, appBuild)
                .uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.62.sp,
            ),
            color = ink4,
        )
    }
}
