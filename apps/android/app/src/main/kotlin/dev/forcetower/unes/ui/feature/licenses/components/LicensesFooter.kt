package dev.forcetower.unes.ui.feature.licenses.components

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R

// Closing signature — bullet · "unes" · bullet, then version/build, then the
// "em conformidade com os termos…" line. Mirrors `LicensesSignature` (iOS).
// Drops the JSX prototype's "Baixar SBOM completo" pill since we don't expose
// a downloadable artifact.
@Composable
internal fun LicensesFooter(
    appVersion: String,
    appBuild: String,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val ink2 = MaterialTheme.colorScheme.onSurface
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accent))
            Text(
                text = stringResource(R.string.licenses_footer_signature),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    letterSpacing = (-0.16).sp,
                    fontStyle = FontStyle.Italic,
                ),
                color = ink2,
            )
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accent))
        }
        Text(
            text = stringResource(R.string.licenses_footer_build_format, appVersion, appBuild),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.62.sp,
            ),
            color = ink4,
        )
        Text(
            text = stringResource(R.string.licenses_footer_compliance),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.08.sp,
                lineHeight = 14.sp,
            ),
            color = ink4,
            textAlign = TextAlign.Center,
        )
    }
}
