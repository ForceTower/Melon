package dev.forcetower.unes.ui.feature.licenses.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.melon

// Dark mesh card with the heart icon and the "obrigado coletivo" line.
// Mirrors `LicensesTributeCard` (iOS) and `LicTribute` (JSX) — fixed dark
// surface in both appearances since the rose mesh + amber accent only read on
// the deep plum background. Casts a soft drop shadow so it lifts off the
// cream surface in light mode.
@Composable
internal fun LicensesTributeCard(modifier: Modifier = Modifier) {
    val darkBg = MaterialTheme.melon.brand.alwaysDarkBg
    val cream = MaterialTheme.melon.fixed.surfaceLight
    val amber = MaterialTheme.melon.brand.amber
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(darkBg),
    ) {
        Mesh(
            variant = MeshVariant.Rose,
            intensity = 0.6f,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        0f to darkBg.copy(alpha = 0.4f),
                        1f to darkBg.copy(alpha = 0.85f),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            HeartBadge(amber = amber)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.licenses_tribute_eyebrow),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.33.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = cream.copy(alpha = 0.55f),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tributeAnnotated(cream = cream, amber = amber),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        letterSpacing = (-0.17).sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun tributeAnnotated(cream: Color, amber: Color) = buildAnnotatedString {
    val lead = stringResource(R.string.licenses_tribute_body_lead)
    val accent = stringResource(R.string.licenses_tribute_body_accent)
    val tail = stringResource(R.string.licenses_tribute_body_tail)
    withStyle(SpanStyle(color = cream)) { append(lead) }
    withStyle(SpanStyle(color = amber, fontStyle = FontStyle.Italic)) { append(accent) }
    withStyle(SpanStyle(color = cream)) { append(tail) }
}

@Composable
private fun HeartBadge(amber: Color) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .background(amber.copy(alpha = 0.2f))
            .border(1.dp, amber.copy(alpha = 0.3f), shape),
        contentAlignment = Alignment.Center,
    ) {
        LicensesIcon(
            glyph = LicensesGlyph.Heart,
            color = amber,
            modifier = Modifier.size(15.dp),
        )
    }
}
