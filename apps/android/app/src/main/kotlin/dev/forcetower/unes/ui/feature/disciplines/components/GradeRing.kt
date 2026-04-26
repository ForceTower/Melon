package dev.forcetower.unes.ui.feature.disciplines.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.DisciplineScoreColor

// Circular progress toward 10, with the score rendered inside. Used both on
// the list cards and on the detail screen's grades headline. Mirrors iOS
// `GradeRing` — same stroke logic, same "—" placeholder when no score.
@Composable
internal fun GradeRing(
    score: Double?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    stroke: Dp = 4.dp,
    color: Color? = null,
) {
    val target = (score?.let { (it / 10.0).coerceIn(0.0, 1.0) } ?: 0.0).toFloat()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(target) {
        progress.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 700),
        )
    }

    val track = MaterialTheme.melon.surface.line
    val hue = color ?: DisciplineScoreColor.colorFor(score)
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.onBackground

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val side = this.size.minDimension
            val arcSize = Size(side - strokePx, side - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx),
            )
            // Rotate -90 so the arc starts at the top instead of the 3-o'clock
            // position (Compose's default angle origin).
            rotate(degrees = -90f, pivot = center) {
                drawArc(
                    color = hue,
                    startAngle = 0f,
                    sweepAngle = 360f * progress.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
        if (score == null) {
            Text(
                text = "—",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = (size.value * 0.38f).sp,
                    lineHeight = (size.value * 0.38f).sp,
                ),
                color = ink4,
            )
        } else {
            Text(
                text = String.format(java.util.Locale.US, "%.1f", score),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = (size.value * 0.38f).sp,
                    lineHeight = (size.value * 0.38f).sp,
                    letterSpacing = (-0.02f * (size.value * 0.38f)).sp,
                ),
                color = ink,
            )
        }
    }
}
