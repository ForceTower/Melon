package dev.forcetower.unes.ui.feature.licenses.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon

// Top chrome of the Licenças screen. Mirrors `LicensesEditorialHeader` on iOS
// and the JSX `LicHeader`: back pill + monospace meta on the right, then the
// "Licenças open source" editorial title with the second word italicised in
// the accent color, and a short subtitle paragraph.
@Composable
internal fun LicensesHeader(
    onBack: () -> Unit,
    totalPackages: Int,
    appVersion: String,
    appBuild: String,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackPill(onBack = onBack)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.licenses_header_meta_format, appBuild, appVersion),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.33.sp,
                ),
                color = ink4,
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.licenses_header_eyebrow_format, totalPackages),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.44.sp,
            ),
            color = ink3,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = titleAnnotated(ink = ink, accent = accent),
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 40.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.8).sp,
            ),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.licenses_header_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            color = ink3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun titleAnnotated(ink: Color, accent: Color) = buildAnnotatedString {
    val lead = stringResource(R.string.licenses_header_title_lead)
    val tail = stringResource(R.string.licenses_header_title_accent)
    withStyle(SpanStyle(color = ink)) { append(lead) }
    withStyle(SpanStyle(color = accent, fontStyle = FontStyle.Italic)) { append(tail) }
}

@Composable
private fun BackPill(onBack: () -> Unit) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val description = stringResource(R.string.licenses_back)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(card)
            .border(1.dp, cardLine, CircleShape)
            .clickable(onClick = onBack)
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        LicensesIcon(
            glyph = LicensesGlyph.ChevronLeft,
            color = ink,
            modifier = Modifier.size(17.dp),
        )
    }
}
