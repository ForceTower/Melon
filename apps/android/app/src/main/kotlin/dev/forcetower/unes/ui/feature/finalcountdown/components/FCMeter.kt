package dev.forcetower.unes.ui.feature.finalcountdown.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.finalcountdown.FCTone
import dev.forcetower.unes.ui.feature.finalcountdown.FinalCountdownMath
import dev.forcetower.unes.ui.feature.finalcountdown.fcToneBackground
import kotlin.math.cos
import kotlin.math.sin

// 240° radial dial (−210° → 30°) with zones for the three outcome bands:
// reprovação (0–3, coral), final (3–7, amber), aprovação (7–10, green).
// Ticks every integer; major labels on 0/3/5/7/10. The needle eases into
// the new average via `MelonMotion.spring()`. Ported from iOS `FCMeter`.
//
// Lives inside the verdict hero, which is always painted with dark
// gradients regardless of the system theme — so every text/needle color
// here is a *fixed* light value (not `colorScheme.onBackground`, which
// flips dark in light mode and would render invisibly on the hero).
@Composable
internal fun FCMeter(
    avg: Double?,
    tone: FCTone,
    modifier: Modifier = Modifier,
) {
    val heroInk = MaterialTheme.melon.fixed.surfaceLight
    val heroInkSoft = heroInk.copy(alpha = 0.55f)
    val heroInkFaint = heroInk.copy(alpha = 0.40f)
    val heroTrack = Color.White.copy(alpha = 0.08f)
    val activeColor = fcToneBackground(tone)
    val coral = fcToneBackground(FCTone.Coral)
    val amber = fcToneBackground(FCTone.Amber)
    val green = fcToneBackground(FCTone.Green)

    val targetPct = if (avg == null) 0f else (avg.coerceIn(0.0, 10.0) / 10.0).toFloat()
    val animatedPct by animateFloatAsState(
        targetValue = targetPct,
        animationSpec = MelonMotion.spring(),
        label = "fc-meter-needle",
    )

    Box(modifier = modifier.size(MeterCanvas)) {
        Canvas(modifier = Modifier.size(MeterCanvas)) {
            val radius = MeterRadiusDp.toPx()
            val cx = size.width / 2f
            val cy = size.height / 2f + 10.dp.toPx()
            val center = Offset(cx, cy)
            val strokePxFloat = MeterStrokeDp.toPx()
            val strokeStyle = Stroke(width = strokePxFloat, cap = StrokeCap.Round)

            // Track — full 240° sweep, drawn first so the zones overlay it.
            drawArcStroke(
                center = center,
                radius = radius,
                fromPct = 0f,
                toPct = 1f,
                color = heroTrack,
                stroke = strokeStyle,
            )
            // Outcome zones: red (0–3), amber (3–7), green (7–10).
            drawArcStroke(center, radius, 0f, FailZone, coral.copy(alpha = 0.15f), strokeStyle)
            drawArcStroke(center, radius, FailZone, PassZone, amber.copy(alpha = 0.20f), strokeStyle)
            drawArcStroke(center, radius, PassZone, 1f, green.copy(alpha = 0.20f), strokeStyle)

            // Active fill — animates from 0 up to the current grade.
            if (avg != null && animatedPct > 0f) {
                drawArcStroke(center, radius, 0f, animatedPct, activeColor, strokeStyle)
            }

            // Ticks + labels around the dial.
            for (v in 0..10) {
                val p = v / 10f
                val major = v % 5 == 0 || v == 3 || v == 7
                val angleRad = (StartAngle + p * Sweep).toDouble() * Math.PI / 180.0
                val r1 = radius + 6.dp.toPx()
                val r2 = radius + (if (major) 14 else 10).dp.toPx()
                val tickStart = Offset(
                    x = cx + r1 * cos(angleRad).toFloat(),
                    y = cy + r1 * sin(angleRad).toFloat(),
                )
                val tickEnd = Offset(
                    x = cx + r2 * cos(angleRad).toFloat(),
                    y = cy + r2 * sin(angleRad).toFloat(),
                )
                drawLine(
                    color = if (major) heroInkSoft else heroInkFaint,
                    start = tickStart,
                    end = tickEnd,
                    strokeWidth = (if (major) 1.4f else 1f).dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // Needle + hub when there's an average to show.
            if (avg != null) {
                val needleAngleRad = (StartAngle + animatedPct * Sweep).toDouble() * Math.PI / 180.0
                val nR = radius - 6.dp.toPx()
                val tip = Offset(
                    x = cx + nR * cos(needleAngleRad).toFloat(),
                    y = cy + nR * sin(needleAngleRad).toFloat(),
                )
                drawLine(
                    color = heroInk,
                    start = center,
                    end = tip,
                    strokeWidth = 2.4f.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(color = heroInk, radius = 6.dp.toPx(), center = center)
                drawCircle(color = activeColor, radius = 2.5f.dp.toPx(), center = center)

                drawCircle(color = activeColor, radius = 3.5f.dp.toPx(), center = tip)
                drawCircle(
                    color = heroInk,
                    radius = 3.5f.dp.toPx(),
                    center = tip,
                    style = Stroke(width = 1.5f.dp.toPx()),
                )
            }
        }

        // Major labels (0/3/5/7/10) are placed via a Box layer so we get the
        // platform text rasterizer rather than rolling our own glyph in
        // Canvas — the iOS impl uses `Text.measure`; on Android using a Box
        // overlay reads cleaner.
        TickLabels(color = heroInkSoft)

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .height(MeterCanvas),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "MÉDIA ATUAL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.62.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = heroInkFaint,
            )
            Text(
                text = FinalCountdownMath.formatGrade(avg),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 52.sp,
                    lineHeight = 52.sp,
                    letterSpacing = (-1.56).sp,
                ),
                color = heroInk,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TickLabels(color: Color) {
    // Major labels reuse the same angular math as the dial so they line up
    // perfectly with the major ticks (0, 3, 5, 7, 10).
    val majors = listOf(0, 3, 5, 7, 10)
    Box(modifier = Modifier.size(MeterCanvas)) {
        majors.forEach { v ->
            val p = v / 10f
            val angleRad = (StartAngle + p * Sweep).toDouble() * Math.PI / 180.0
            val rL = MeterRadiusDp + 22.dp
            val cx = MeterCanvas / 2
            val cy = MeterCanvas / 2 + 10.dp
            val xDp = cx + rL * cos(angleRad).toFloat()
            val yDp = cy + rL * sin(angleRad).toFloat()
            Text(
                text = v.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = color,
                modifier = Modifier.absoluteOffsetCentered(xDp = xDp, yDp = yDp),
            )
        }
    }
}

// Center a Text on a (xDp, yDp) coordinate. Implemented as a layout modifier
// so each label centers on its own measured size — no manual width math.
private fun Modifier.absoluteOffsetCentered(
    xDp: androidx.compose.ui.unit.Dp,
    yDp: androidx.compose.ui.unit.Dp,
): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(placeable.width, placeable.height) {
        val xPx = xDp.roundToPx() - placeable.width / 2
        val yPx = yDp.roundToPx() - placeable.height / 2
        placeable.place(xPx, yPx)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArcStroke(
    center: Offset,
    radius: Float,
    fromPct: Float,
    toPct: Float,
    color: Color,
    stroke: Stroke,
) {
    if (toPct <= fromPct) return
    val startAngle = StartAngle + fromPct * Sweep
    val sweepAngle = (toPct - fromPct) * Sweep
    val topLeft = Offset(center.x - radius, center.y - radius)
    val s = Size(radius * 2f, radius * 2f)
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = s,
        style = stroke,
    )
}

private val MeterCanvas = 220.dp
private val MeterRadiusDp = 82.dp
private val MeterStrokeDp = 10.dp

// Visual zone boundaries on the 0..1 sweep. 3/10 → red→amber, 7/10 → amber→green.
private const val FailZone: Float = 0.3f
private const val PassZone: Float = 0.7f

private const val StartAngle: Float = -210f
private const val EndAngle: Float = 30f
private const val Sweep: Float = EndAngle - StartAngle
