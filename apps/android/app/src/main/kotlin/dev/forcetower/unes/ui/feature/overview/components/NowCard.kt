package dev.forcetower.unes.ui.feature.overview.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.overview.OverviewNowClass

@Composable
internal fun NowCard(now: OverviewNowClass, modifier: Modifier = Modifier) {
    val alwaysDark = MaterialTheme.melon.brand.alwaysDarkBg
    val onAlwaysDark = Color(0xFFFBF7F2)
    val countdown = run {
        val h = now.startsInMinutes / 60
        val m = now.startsInMinutes % 60
        if (h > 0) {
            stringResource(R.string.overview_now_countdown_hour_min, h, m)
        } else {
            stringResource(R.string.overview_now_countdown_min, m)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(alwaysDark),
    ) {
        Mesh(
            variant = now.meshVariant,
            intensity = 1f,
            modifier = Modifier.fillMaxSize(),
        )
        // Dim veil for contrast.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            alwaysDark.copy(alpha = 0.10f),
                            alwaysDark.copy(alpha = 0.55f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Eyebrow(
                    text = stringResource(R.string.overview_now_eyebrow, countdown),
                    foreground = onAlwaysDark.copy(alpha = 0.7f),
                    pulseColor = MaterialTheme.melon.brand.amber,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = now.timeRange,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                    ),
                    color = onAlwaysDark.copy(alpha = 0.55f),
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = now.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 30.sp,
                    lineHeight = 32.sp,
                    letterSpacing = (-0.45).sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = onAlwaysDark,
            )

            now.topic?.let { topic ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinesGlyph(color = onAlwaysDark.copy(alpha = 0.55f))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = topic,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                        ),
                        color = onAlwaysDark.copy(alpha = 0.78f),
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(onAlwaysDark.copy(alpha = 0.15f)),
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetaCell(
                    glyph = { RoomGlyph(color = onAlwaysDark.copy(alpha = 0.55f)) },
                    label = now.room,
                    foreground = onAlwaysDark.copy(alpha = 0.85f),
                    shrinks = false,
                )
                Spacer(Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(onAlwaysDark.copy(alpha = 0.20f)),
                )
                Spacer(Modifier.width(16.dp))
                MetaCell(
                    glyph = { ProfGlyph(color = onAlwaysDark.copy(alpha = 0.55f)) },
                    label = now.prof.removePrefix("Prof. "),
                    foreground = onAlwaysDark.copy(alpha = 0.85f),
                    shrinks = true,
                )
            }
        }
    }
}

@Composable
private fun Eyebrow(
    text: String,
    foreground: Color,
    pulseColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        PulsingDot(color = pulseColor)
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.8.sp,
            ),
            color = foreground,
        )
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "now-pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun MetaCell(
    glyph: @Composable () -> Unit,
    label: String,
    foreground: Color,
    shrinks: Boolean,
) {
    Row(
        modifier = if (shrinks) Modifier else Modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        glyph()
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = foreground,
            maxLines = 1,
        )
    }
}

@Composable
private fun RoomGlyph(color: Color) {
    Canvas(modifier = Modifier.size(12.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.2f * density, cap = StrokeCap.Round)
        val outline = Path().apply {
            moveTo(w * (2f / 12f), h * (10f / 12f))
            lineTo(w * (2f / 12f), h * (4f / 12f))
            lineTo(w * (6f / 12f), h * (2f / 12f))
            lineTo(w * (10f / 12f), h * (4f / 12f))
            lineTo(w * (10f / 12f), h * (10f / 12f))
        }
        drawPath(outline, color = color, style = stroke)
        val door = Path().apply {
            moveTo(w * (5f / 12f), h * (10f / 12f))
            lineTo(w * (5f / 12f), h * (7f / 12f))
            lineTo(w * (7f / 12f), h * (7f / 12f))
            lineTo(w * (7f / 12f), h * (10f / 12f))
        }
        drawPath(door, color = color, style = stroke)
    }
}

@Composable
private fun ProfGlyph(color: Color) {
    Canvas(modifier = Modifier.size(12.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.2f * density, cap = StrokeCap.Round)
        drawCircle(
            color = color,
            radius = w * (2f / 12f),
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * (4.5f / 12f)),
            style = stroke,
        )
        val shoulders = Path().apply {
            moveTo(w * (2.5f / 12f), h * (10f / 12f))
            quadraticTo(w * 0.5f, h * (6.5f / 12f), w * (9.5f / 12f), h * (10f / 12f))
        }
        drawPath(shoulders, color = color, style = stroke)
    }
}

@Composable
private fun LinesGlyph(color: Color) {
    Canvas(modifier = Modifier.size(12.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.3f * density, cap = StrokeCap.Round)
        listOf(3f, 6f, 9f).forEachIndexed { idx, y ->
            val end = if (idx == 2) 7f else 10f
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(w * (2f / 12f), h * (y / 12f)),
                end = androidx.compose.ui.geometry.Offset(w * (end / 12f), h * (y / 12f)),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
        }
    }
}

