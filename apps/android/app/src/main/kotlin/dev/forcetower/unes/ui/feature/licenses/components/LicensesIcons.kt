package dev.forcetower.unes.ui.feature.licenses.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

// Stroke glyphs used inside the Licenças screen. Same 18-unit viewbox + rounded
// caps as Settings/Me icon sets — kept feature-local so the design system
// catalogue doesn't grow to absorb every per-screen path.
internal enum class LicensesGlyph {
    ChevronLeft, ChevronDown, ChevronRight,
    Search, Close, Heart, Copy, Check, ExternalLink,
}

@Composable
internal fun LicensesIcon(glyph: LicensesGlyph, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawGlyph(glyph, color, size.minDimension)
    }
}

private fun DrawScope.drawGlyph(glyph: LicensesGlyph, color: Color, s: Float) {
    val stroke = Stroke(width = strokeWidth(s), cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (glyph) {
        LicensesGlyph.ChevronLeft -> {
            val path = Path().apply {
                moveTo(s * (11f / 18f), s * (4f / 18f))
                lineTo(s * (6f / 18f), s * (9f / 18f))
                lineTo(s * (11f / 18f), s * (14f / 18f))
            }
            drawPath(path, color = color, style = Stroke(width = strokeWidth(s, base = 1.6f), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        LicensesGlyph.ChevronDown -> {
            val path = Path().apply {
                moveTo(s * (4f / 18f), s * (7f / 18f))
                lineTo(s * (9f / 18f), s * (12f / 18f))
                lineTo(s * (14f / 18f), s * (7f / 18f))
            }
            drawPath(path, color = color, style = Stroke(width = strokeWidth(s, base = 1.6f), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        LicensesGlyph.ChevronRight -> {
            val path = Path().apply {
                moveTo(s * (7f / 18f), s * (4f / 18f))
                lineTo(s * (12f / 18f), s * (9f / 18f))
                lineTo(s * (7f / 18f), s * (14f / 18f))
            }
            drawPath(path, color = color, style = Stroke(width = strokeWidth(s, base = 1.6f), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        LicensesGlyph.Search -> {
            drawCircle(color = color, radius = s * (4.5f / 18f), center = Offset(s * (8f / 18f), s * (8f / 18f)), style = stroke)
            val tail = Path().apply {
                moveTo(s * (11.5f / 18f), s * (11.5f / 18f))
                lineTo(s * (15f / 18f), s * (15f / 18f))
            }
            drawPath(tail, color = color, style = stroke)
        }
        LicensesGlyph.Close -> {
            val cross = Path().apply {
                moveTo(s * (4f / 18f), s * (4f / 18f))
                lineTo(s * (14f / 18f), s * (14f / 18f))
                moveTo(s * (14f / 18f), s * (4f / 18f))
                lineTo(s * (4f / 18f), s * (14f / 18f))
            }
            drawPath(cross, color = color, style = stroke)
        }
        LicensesGlyph.Heart -> {
            val heart = Path().apply {
                moveTo(s * (9f / 18f), s * (14.5f / 18f))
                cubicTo(s * (4f / 18f), s * (11.5f / 18f), s * (4f / 18f), s * (7.5f / 18f), s * (4f / 18f), s * (7.5f / 18f))
                cubicTo(s * (4f / 18f), s * (5.5f / 18f), s * (5.5f / 18f), s * (4.5f / 18f), s * (7f / 18f), s * (4.5f / 18f))
                cubicTo(s * (8f / 18f), s * (4.5f / 18f), s * (8.7f / 18f), s * (5.2f / 18f), s * (9f / 18f), s * (5.7f / 18f))
                cubicTo(s * (9.3f / 18f), s * (5.2f / 18f), s * (10f / 18f), s * (4.5f / 18f), s * (11f / 18f), s * (4.5f / 18f))
                cubicTo(s * (12.5f / 18f), s * (4.5f / 18f), s * (14f / 18f), s * (5.5f / 18f), s * (14f / 18f), s * (7.5f / 18f))
                cubicTo(s * (14f / 18f), s * (7.5f / 18f), s * (14f / 18f), s * (11.5f / 18f), s * (9f / 18f), s * (14.5f / 18f))
                close()
            }
            drawPath(heart, color = color, style = stroke)
        }
        LicensesGlyph.Copy -> {
            val rect = Path().apply {
                moveTo(s * (6f / 18f), s * (6f / 18f))
                lineTo(s * (15f / 18f), s * (6f / 18f))
                lineTo(s * (15f / 18f), s * (15f / 18f))
                lineTo(s * (6f / 18f), s * (15f / 18f))
                close()
            }
            drawPath(rect, color = color, style = stroke)
            val flap = Path().apply {
                moveTo(s * (3f / 18f), s * (11f / 18f))
                lineTo(s * (3f / 18f), s * (4f / 18f))
                lineTo(s * (4f / 18f), s * (3f / 18f))
                lineTo(s * (11f / 18f), s * (3f / 18f))
            }
            drawPath(flap, color = color, style = stroke)
        }
        LicensesGlyph.Check -> {
            val heavy = Stroke(width = strokeWidth(s, base = 1.8f), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val path = Path().apply {
                moveTo(s * (3f / 18f), s * (9.5f / 18f))
                lineTo(s * (6.5f / 18f), s * (13f / 18f))
                lineTo(s * (15f / 18f), s * (5f / 18f))
            }
            drawPath(path, color = color, style = heavy)
        }
        LicensesGlyph.ExternalLink -> {
            val frame = Path().apply {
                moveTo(s * (7f / 18f), s * (3f / 18f))
                lineTo(s * (4f / 18f), s * (3f / 18f))
                lineTo(s * (3f / 18f), s * (4f / 18f))
                lineTo(s * (3f / 18f), s * (14f / 18f))
                lineTo(s * (4f / 18f), s * (15f / 18f))
                lineTo(s * (14f / 18f), s * (15f / 18f))
                lineTo(s * (15f / 18f), s * (14f / 18f))
                lineTo(s * (15f / 18f), s * (11f / 18f))
            }
            drawPath(frame, color = color, style = stroke)
            val arrow = Path().apply {
                moveTo(s * (10f / 18f), s * (3f / 18f))
                lineTo(s * (15f / 18f), s * (3f / 18f))
                lineTo(s * (15f / 18f), s * (8f / 18f))
                moveTo(s * (9f / 18f), s * (9f / 18f))
                lineTo(s * (15f / 18f), s * (3f / 18f))
            }
            drawPath(arrow, color = color, style = stroke)
        }
    }
}

private fun strokeWidth(canvasSize: Float, base: Float = 1.4f): Float =
    base * (canvasSize / 18f).coerceAtLeast(0.6f)
