package dev.forcetower.unes.ui.feature.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import androidx.compose.foundation.Canvas

@Composable
internal fun OverviewHeader(
    greeting: String,
    name: String,
    avatarInitial: String,
    dateEyebrow: String,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val eyebrowPrefix = stringResource(R.string.overview_date_eyebrow_prefix)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "$eyebrowPrefix $dateEyebrow",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.44.sp,
                ),
                color = ink3,
            )
            Text(
                text = buildGreeting(greeting = greeting, name = name, ink = ink, accent = accent),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.6).sp,
                    fontWeight = FontWeight.Normal,
                ),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AvatarButton(
                initial = avatarInitial,
                contentDescription = stringResource(R.string.overview_avatar_label),
            )
        }
    }
}

@Composable
private fun buildGreeting(
    greeting: String,
    name: String,
    ink: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color,
) = buildAnnotatedString {
    withStyle(SpanStyle(color = ink)) { append("$greeting, ") }
    withStyle(SpanStyle(color = accent, fontStyle = FontStyle.Italic)) { append(name) }
}

@Composable
private fun CircleIconButton(
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(1.dp, MaterialTheme.melon.surface.cardLine, CircleShape)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun AvatarButton(initial: String, contentDescription: String) {
    val brand = MaterialTheme.melon.brand
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(1.dp, MaterialTheme.melon.surface.cardLine, CircleShape)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(brand.coral, brand.amber),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = androidx.compose.ui.graphics.Color(0xFFFBF7F2),
            )
        }
    }
}

@Composable
private fun SearchGlyph() {
    val ink = MaterialTheme.colorScheme.onBackground
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val cx = w * (8f / 18f)
        val cy = h * (8f / 18f)
        val r = w * (5f / 18f)
        val stroke = Stroke(width = 1.5f * density, cap = StrokeCap.Round)
        drawCircle(color = ink, radius = r, center = androidx.compose.ui.geometry.Offset(cx, cy), style = stroke)
        val handle = Path().apply {
            moveTo(w * (15f / 18f), h * (15f / 18f))
            lineTo(w * (12f / 18f), h * (12f / 18f))
        }
        drawPath(handle, color = ink, style = stroke)
    }
}
