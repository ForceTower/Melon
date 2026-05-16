package dev.forcetower.unes.ui.feature.calendar.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.forcetower.unes.ui.feature.calendar.CalendarCategory

// Hand-rolled SVG-equivalent glyphs — three shapes: holiday (sun), exam
// (folded paper), deadline (bowtie hourglass). Mirrors `CalCatGlyph` in
// `screens-calendar.jsx`. Each shape is drawn against a 14×14 coordinate
// system and scaled to the requested size.
@Composable
internal fun CalCategoryGlyph(
    category: CalendarCategory,
    color: Color,
    size: Dp = 14.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.width / 14f
        val stroke = Stroke(
            width = 1.5f * density,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        when (category) {
            CalendarCategory.Holiday -> drawHoliday(color, scale, stroke)
            CalendarCategory.Exam -> drawExam(color, scale, stroke)
            CalendarCategory.Deadline -> drawDeadline(color, scale, stroke)
        }
    }
}

private fun DrawScope.drawHoliday(color: Color, scale: Float, stroke: Stroke) {
    drawCircle(
        color = color,
        radius = 4f * scale,
        center = Offset(7f * scale, 7f * scale),
        style = stroke,
    )
    val rays = Path()
    val pairs = listOf(
        7f to 1.5f to (7f to 3f),
        7f to 11f to (7f to 12.5f),
        1.5f to 7f to (3f to 7f),
        11f to 7f to (12.5f to 7f),
        4.2f to 4.2f to (3.1f to 3.1f),
        10.9f to 10.9f to (9.8f to 9.8f),
        4.2f to 9.8f to (3.1f to 10.9f),
        10.9f to 3.1f to (9.8f to 4.2f),
    )
    pairs.forEach { (a, b) ->
        rays.moveTo(a.first * scale, a.second * scale)
        rays.lineTo(b.first * scale, b.second * scale)
    }
    drawPath(rays, color, style = stroke)
}

private fun DrawScope.drawExam(color: Color, scale: Float, stroke: Stroke) {
    val outline = Path().apply {
        moveTo(3f * scale, 2f * scale)
        lineTo(9f * scale, 2f * scale)
        lineTo(11f * scale, 4f * scale)
        lineTo(11f * scale, 12.5f * scale)
        lineTo(2f * scale, 12.5f * scale)
        lineTo(2f * scale, 2f * scale)
        close()
    }
    drawPath(outline, color, style = stroke)
    val details = Path().apply {
        // Folded corner.
        moveTo(9f * scale, 2f * scale)
        lineTo(9f * scale, 4.5f * scale)
        lineTo(11f * scale, 4.5f * scale)
        // Three rule lines.
        listOf(7f to 5f, 9.5f to 5f, 12f to 3f).forEach { (y, len) ->
            moveTo(4.5f * scale, y * scale)
            lineTo((4.5f + len) * scale, y * scale)
        }
    }
    drawPath(details, color, style = stroke)
}

private fun DrawScope.drawDeadline(color: Color, scale: Float, stroke: Stroke) {
    val bars = Path().apply {
        moveTo(3.5f * scale, 2f * scale)
        lineTo(10.5f * scale, 2f * scale)
        moveTo(3.5f * scale, 12f * scale)
        lineTo(10.5f * scale, 12f * scale)
    }
    drawPath(bars, color, style = stroke)
    val curves = Path().apply {
        moveTo(4.5f * scale, 2f * scale)
        quadraticTo(4.5f * scale, 7f * scale, 9.5f * scale, 12f * scale)
        moveTo(9.5f * scale, 2f * scale)
        quadraticTo(9.5f * scale, 7f * scale, 4.5f * scale, 12f * scale)
    }
    drawPath(curves, color, style = stroke)
}
