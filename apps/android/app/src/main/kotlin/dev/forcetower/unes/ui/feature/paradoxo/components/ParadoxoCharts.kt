package dev.forcetower.unes.ui.feature.paradoxo.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoSemesterMean
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.paradoxo.ParadoxoFormat

// Hand-drawn charts for the Paradoxo aggregates, following the dc
// `ParadoxoScreen` geometry: semester-mean line chart with per-point value
// labels, the 0–10 grade histogram, the teacher donut and the tiny
// sparklines. All colors arrive resolved from the theme.

private const val ChartMaxGrade = 10.0

// Semester-mean line chart block: rotated "Média" caption on the left, the
// plot with dashed gridlines at 10/5/0 + per-point value labels, semester
// labels under the axis and the "Semestre" caption. dc: viewBox 328×172,
// labels every 1/2/3/6 points depending on the series length.
@Composable
internal fun ParadoxoHistoryChart(
    history: List<ParadoxoSemesterMean>,
    tone: Color,
    modifier: Modifier = Modifier,
    plotHeight: Dp = 172.dp,
) {
    val measurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.melon.surface.line
    val tickColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.outline
    val meanCaption = stringResource(R.string.paradoxo_chart_axis_mean).uppercase()
    val semesterCaption = stringResource(R.string.paradoxo_chart_axis_semester).uppercase()

    val tickStyle = TextStyle(fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = tickColor)
    val captionStyle = TextStyle(
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.95.sp,
        color = tickColor,
    )
    val xLabelStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = labelColor)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(plotHeight + 36.dp),
    ) {
        val n = history.size
        if (n == 0) return@Canvas

        val leftPad = 16.dp.toPx()
        val rightPad = 26.dp.toPx()
        val plotH = plotHeight.toPx()
        val topY = plotH * (20f / 172f)
        val bottomY = plotH * (156f / 172f)
        val plotLeft = leftPad
        val plotRight = size.width - rightPad

        fun px(i: Int): Float =
            if (n == 1) (plotLeft + plotRight) / 2f
            else plotLeft + (i.toFloat() / (n - 1)) * (plotRight - plotLeft)

        fun py(v: Double): Float =
            bottomY - ((v / ChartMaxGrade).toFloat() * (bottomY - topY))

        // Gridlines + right-edge tick labels for 10 / 5 / 0.
        val dash = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 4.dp.toPx()))
        listOf(10.0, 5.0, 0.0).forEach { tick ->
            val y = py(tick)
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dash,
            )
            val layout = measurer.measure(AnnotatedString(tick.toInt().toString()), tickStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(size.width - layout.size.width, y - layout.size.height / 2f),
            )
        }

        // The series line.
        val points = history.mapIndexed { i, sem -> Offset(px(i), py(sem.mean)) }
        if (points.size > 1) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = path,
                color = tone,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        } else {
            drawCircle(color = tone, radius = 3.dp.toPx(), center = points.first())
        }

        // Per-point value labels, alternating above/below; peak and trough
        // stay above and pick up the tone.
        val means = history.map { it.mean }
        val peak = means.indices.maxBy { means[it] }
        val trough = means.indices.minBy { means[it] }
        history.forEachIndexed { i, sem ->
            val marker = i == peak || i == trough
            val above = i % 2 == 0 || marker
            val style = TextStyle(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (marker) tone else labelColor,
            )
            val layout = measurer.measure(AnnotatedString(ParadoxoFormat.grade(sem.mean)), style)
            val y = points[i].y + if (above) -(9.dp.toPx()) else 13.dp.toPx()
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    (points[i].x - layout.size.width / 2f)
                        .coerceIn(0f, size.width - layout.size.width),
                    y - layout.size.height / 2f,
                ),
            )
        }

        // Semester labels along the x axis (thinned on long series).
        val labelEvery = when {
            n > 16 -> 6
            n > 8 -> 3
            n > 4 -> 2
            else -> 1
        }
        val xLabelY = plotH + 6.dp.toPx()
        history.forEachIndexed { i, sem ->
            if (sem.semester.isBlank()) return@forEachIndexed
            if (i % labelEvery != 0 && i != n - 1) return@forEachIndexed
            val layout = measurer.measure(AnnotatedString(sem.semester), xLabelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    (points[i].x - layout.size.width / 2f)
                        .coerceIn(0f, size.width - layout.size.width),
                    xLabelY,
                ),
            )
        }

        // Axis captions: "Semestre" centered under the labels, "Média"
        // rotated along the left edge.
        val semLayout = measurer.measure(AnnotatedString(semesterCaption), captionStyle)
        drawText(
            textLayoutResult = semLayout,
            topLeft = Offset(
                (plotLeft + plotRight - semLayout.size.width) / 2f,
                size.height - semLayout.size.height,
            ),
        )
        val meanLayout = measurer.measure(AnnotatedString(meanCaption), captionStyle)
        rotate(degrees = -90f, pivot = Offset(0f, plotH / 2f)) {
            drawText(
                textLayoutResult = meanLayout,
                topLeft = Offset(-meanLayout.size.width / 2f, plotH / 2f - meanLayout.size.height),
            )
        }
    }
}

// Grade histogram (11 buckets, 0…10). Bars are the tone at 23% alpha; the
// bucket the student's own grade falls into paints solid with a dashed rule
// through it. Bucket labels render as a Row so they stay crisp text.
@Composable
internal fun ParadoxoDistribution(
    distribution: List<Double>,
    tone: Color,
    myGrade: Double?,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 128.dp,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            if (distribution.isEmpty()) return@Canvas
            val padTop = if (myGrade != null) 18.dp.toPx() else 6.dp.toPx()
            val innerH = size.height - padTop
            val max = distribution.max().takeIf { it > 0 } ?: 1.0
            val slot = size.width / distribution.size
            val gap = 3.dp.toPx()
            val myBucket = myGrade?.let {
                kotlin.math.round(it).toInt().coerceIn(0, distribution.size - 1)
            }
            distribution.forEachIndexed { i, share ->
                val h = ((share / max).toFloat() * innerH).coerceAtLeast(2.dp.toPx())
                drawRoundRect(
                    color = if (i == myBucket) tone else tone.copy(alpha = 0.23f),
                    topLeft = Offset(i * slot + gap / 2f, size.height - h),
                    size = Size(slot - gap, h),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
            if (myGrade != null) {
                val x = ((myGrade / ChartMaxGrade).toFloat() * size.width)
                    .coerceIn(1.dp.toPx(), size.width - 1.dp.toPx())
                drawLine(
                    color = tone,
                    start = Offset(x, 2.dp.toPx()),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(3.dp.toPx(), 3.dp.toPx()),
                    ),
                )
            }
        }
        Row(modifier = Modifier.padding(top = 6.dp)) {
            distribution.indices.forEach { bucket ->
                Text(
                    text = bucket.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// Teacher hero donut — full ring, track on surface3, fill sweep proportional
// to mean/10, truncated mean + student count in the middle.
@Composable
internal fun ParadoxoDonut(
    mean: Double,
    caption: String,
    tone: Color,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val fraction by animateFloatAsState(
        targetValue = (mean / ChartMaxGrade).toFloat().coerceIn(0f, 1f),
        animationSpec = MelonMotion.easeSlow(),
        label = "paradoxoDonut",
    )
    Box(modifier = modifier.size(172.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round)
            val inset = 6.5.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            if (fraction > 0f) {
                drawArc(
                    color = tone,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = ParadoxoFormat.grade(mean),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 52.sp,
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

// Tiny min–max normalized polyline (home rows: 42×18; teacher tile: 54×14).
@Composable
internal fun ParadoxoSparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 1.6.dp,
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val range = (values.max() - min).takeIf { it > 0 } ?: 1.0
        val pad = strokeWidth.toPx()
        val stepX = (size.width - pad * 2) / (values.size - 1)
        val path = androidx.compose.ui.graphics.Path()
        values.forEachIndexed { i, v ->
            val x = pad + stepX * i
            val y = pad + (1f - ((v - min) / range).toFloat()) * (size.height - pad * 2)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
