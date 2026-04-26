package dev.forcetower.unes.ui.feature.disciplines.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon

// Dashed placeholder card for a semester whose data hasn't been fetched yet.
// Tapping starts `SyncSemesterUseCase`; while in flight it shows a spinning
// indicator. Mirrors iOS `UndownloadedSemesterCard`.
@Composable
internal fun UndownloadedSemesterCard(
    semesterCode: String,
    estimatedCount: Int?,
    isLoading: Boolean,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val surface2 = MaterialTheme.colorScheme.surfaceVariant
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .dashedBorder(cardLine, shape)
            .clickable(enabled = !isLoading, onClick = onDownload)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(surface2),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                SpinnerIcon(color = ink3)
            } else {
                DownloadIcon(color = ink3)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = semesterCode,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 18.sp,
                        lineHeight = 18.sp,
                        letterSpacing = (-0.18).sp,
                    ),
                    color = if (isLoading) ink3 else ink,
                )
                Text(
                    text = countLabel(isLoading = isLoading, estimatedCount = estimatedCount),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                    ),
                    color = ink4,
                )
            }
            Text(
                text = if (isLoading) {
                    stringResource(R.string.disciplines_undownloaded_loading_subtitle)
                } else {
                    stringResource(R.string.disciplines_undownloaded_idle_subtitle)
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = ink3,
            )
        }
        if (!isLoading) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, cardLine, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.disciplines_undownloaded_cta),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    ),
                    color = ink3,
                )
            }
        }
    }
}

@Composable
private fun countLabel(isLoading: Boolean, estimatedCount: Int?): String {
    if (isLoading) return stringResource(R.string.disciplines_undownloaded_count_loading)
    if (estimatedCount == null) return stringResource(R.string.disciplines_undownloaded_count_unknown)
    return pluralStringResource(
        R.plurals.disciplines_undownloaded_count_estimated,
        estimatedCount,
        estimatedCount,
    )
}

@Composable
private fun SpinnerIcon(color: Color) {
    val rotation by rememberInfiniteTransition(label = "spinner")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "spinnerRotation",
        )
    Canvas(
        modifier = Modifier
            .size(16.dp)
            .rotate(rotation),
    ) {
        val stroke = 1.5.dp.toPx()
        // Faded full ring + a 90° lifted arc tracing the spin direction.
        drawCircle(
            color = color.copy(alpha = 0.25f),
            radius = (size.minDimension - stroke) / 2f,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
    }
}

@Composable
private fun DownloadIcon(color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        // Down arrow + base line (matches iOS `arrow.down.to.line`).
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.125f),
            end = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.6875f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.469f),
            end = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.6875f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.6875f),
            end = androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.469f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.1875f, h * 0.8125f),
            end = androidx.compose.ui.geometry.Offset(w * 0.8125f, h * 0.8125f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

// Dashed border drawn directly so we don't ship a third-party shape just for
// the placeholder semester card. Honors the same 18dp corner radius as the
// parent shape so the dashes follow the rounded outline cleanly.
private fun Modifier.dashedBorder(
    color: Color,
    @Suppress("UNUSED_PARAMETER") shape: RoundedCornerShape,
): Modifier = this.then(
    Modifier.drawWithCache {
        val strokePx = 1.dp.toPx()
        val dash = floatArrayOf(4.dp.toPx(), 4.dp.toPx())
        val cornerPx = 18.dp.toPx()
        val inset = strokePx / 2f
        val rectSize = Size(size.width - strokePx, size.height - strokePx)
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
        val style = Stroke(width = strokePx, pathEffect = PathEffect.dashPathEffect(dash, 0f))
        onDrawWithContent {
            drawContent()
            drawRoundRect(
                color = color,
                topLeft = topLeft,
                size = rectSize,
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                style = style,
            )
        }
    },
)
