package dev.forcetower.unes.ui.feature.schedule.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Stroke-only glyphs used by the location row and the empty-state row.
// Mirrors the inline SVGs in `screens-schedule.jsx` — kept locally so the
// schedule feature doesn't depend on Material's filled icon set.

@Composable
internal fun ModuloGlyph(color: Color, size: Dp = 11.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = 1.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val outline = Path().apply {
            moveTo(w * (2f / 11f), h)
            lineTo(w * (2f / 11f), h * (3.5f / 11f))
            lineTo(w * (5.5f / 11f), h * (1.7f / 11f))
            lineTo(w * (9f / 11f), h * (3.5f / 11f))
            lineTo(w * (9f / 11f), h)
        }
        drawPath(outline, color = color, style = stroke)
        val door = Path().apply {
            moveTo(w * (4f / 11f), h)
            lineTo(w * (4f / 11f), h * (7f / 11f))
            lineTo(w * (7f / 11f), h * (7f / 11f))
            lineTo(w * (7f / 11f), h)
        }
        drawPath(door, color = color, style = stroke)
    }
}

@Composable
internal fun RoomGlyph(color: Color, size: Dp = 11.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = 1.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val rect = Path().apply {
            moveTo(w * (2f / 11f), h * (2f / 11f))
            lineTo(w * (9f / 11f), h * (2f / 11f))
            lineTo(w * (9f / 11f), h * (9f / 11f))
            lineTo(w * (2f / 11f), h * (9f / 11f))
            close()
        }
        drawPath(rect, color = color, style = stroke)
    }
}

@Composable
internal fun CampusGlyph(color: Color, size: Dp = 11.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = 1.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(
            color = color,
            radius = w * (4f / 11f),
            center = Offset(w * 0.5f, h * 0.5f),
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset(w * (2f / 11f), h * 0.5f),
            end = Offset(w * (9f / 11f), h * 0.5f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        val meridian = Path().apply {
            moveTo(w * 0.5f, h * (1.5f / 11f))
            quadraticTo(w * (8f / 11f), h * 0.5f, w * 0.5f, h * (9.5f / 11f))
        }
        drawPath(meridian, color = color, style = stroke)
        val meridianMirror = Path().apply {
            moveTo(w * 0.5f, h * (1.5f / 11f))
            quadraticTo(w * (3f / 11f), h * 0.5f, w * 0.5f, h * (9.5f / 11f))
        }
        drawPath(meridianMirror, color = color, style = stroke)
    }
}

@Composable
internal fun PinGlyph(color: Color, size: Dp = 11.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = 1.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val pin = Path().apply {
            moveTo(w * 0.5f, h)
            cubicTo(
                w * (2f / 11f), h * (6.5f / 11f),
                w * (2f / 11f), h * (4.3f / 11f),
                w * 0.5f, h * (1f / 11f),
            )
            cubicTo(
                w * (9f / 11f), h * (4.3f / 11f),
                w * (9f / 11f), h * (6.5f / 11f),
                w * 0.5f, h,
            )
            close()
        }
        drawPath(pin, color = color, style = stroke)
        drawCircle(
            color = color,
            radius = w * (1.1f / 11f),
            center = Offset(w * 0.5f, h * (4.3f / 11f)),
            style = stroke,
        )
    }
}

@Composable
internal fun WarningGlyph(color: Color, size: Dp = 11.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = 1.1f * density, cap = StrokeCap.Round)
        drawCircle(
            color = color,
            radius = w * (4.5f / 11f),
            center = Offset(w * 0.5f, h * 0.5f),
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * (3.2f / 11f)),
            end = Offset(w * 0.5f, h * (5.8f / 11f)),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = color,
            radius = stroke.width * 0.5f,
            center = Offset(w * 0.5f, h * (7.6f / 11f)),
        )
    }
}

@Composable
internal fun TopicLinesGlyph(color: Color, size: Dp = 10.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = 1.2f * density, cap = StrokeCap.Round)
        val rows = listOf(2.5f, 5f, 7.5f)
        rows.forEachIndexed { idx, y ->
            val end = if (idx == rows.lastIndex) 5f else 8.5f
            drawLine(
                color = color,
                start = Offset(w * (1.5f / 10f), h * (y / 10f)),
                end = Offset(w * (end / 10f), h * (y / 10f)),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
internal fun TodayDotGlyph(color: Color, size: Dp = 10.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = 1f * density)
        drawCircle(
            color = color.copy(alpha = 0.4f),
            radius = w * 0.4f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = stroke,
        )
        drawCircle(
            color = color,
            radius = w * 0.2f,
            center = Offset(w * 0.5f, h * 0.5f),
        )
    }
}
