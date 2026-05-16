package dev.forcetower.unes.ui.feature.settings.components

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

// Stroke icons used inside the Settings rows. Same 18-unit viewbox + rounded
// caps as the Me feature's `MeIcons`; kept inside the Settings feature so the
// glyph set can match the JSX prototype `CfgIcon` switch one-to-one without
// inflating the design system catalogue.
internal enum class SettingsGlyph {
    Key, Eye, EyeOff, Copy, Check, Shield, Chevron, ChevronLeft,
    Megaphone, Users, Envelope, Sparkle, Pencil, Calendar, Pin, Book, Tag,
}

@Composable
internal fun SettingsIcon(glyph: SettingsGlyph, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawGlyph(glyph, color, size.minDimension)
    }
}

private fun DrawScope.drawGlyph(glyph: SettingsGlyph, color: Color, s: Float) {
    val stroke = Stroke(width = strokeWidth(s), cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (glyph) {
        SettingsGlyph.Key -> {
            drawCircle(color = color, radius = s * (3.5f / 18f), center = Offset(s * (6f / 18f), s * (9f / 18f)), style = stroke)
            val tail = Path().apply {
                moveTo(s * (9.3f / 18f), s * (9f / 18f))
                lineTo(s * (15f / 18f), s * (9f / 18f))
                moveTo(s * (13f / 18f), s * (9f / 18f))
                lineTo(s * (13f / 18f), s * (11.5f / 18f))
                moveTo(s * (15f / 18f), s * (9f / 18f))
                lineTo(s * (15f / 18f), s * (11f / 18f))
            }
            drawPath(tail, color = color, style = stroke)
        }
        SettingsGlyph.Eye -> {
            val outer = Path().apply {
                moveTo(s * (1.5f / 18f), s * (9f / 18f))
                cubicTo(s * (3f / 18f), s * (5f / 18f), s * (6f / 18f), s * (4f / 18f), s * (9f / 18f), s * (4f / 18f))
                cubicTo(s * (12f / 18f), s * (4f / 18f), s * (15f / 18f), s * (5f / 18f), s * (16.5f / 18f), s * (9f / 18f))
                cubicTo(s * (15f / 18f), s * (13f / 18f), s * (12f / 18f), s * (14f / 18f), s * (9f / 18f), s * (14f / 18f))
                cubicTo(s * (6f / 18f), s * (14f / 18f), s * (3f / 18f), s * (13f / 18f), s * (1.5f / 18f), s * (9f / 18f))
                close()
            }
            drawPath(outer, color = color, style = stroke)
            drawCircle(color = color, radius = s * (2.2f / 18f), center = Offset(s / 2f, s / 2f), style = stroke)
        }
        SettingsGlyph.EyeOff -> {
            val slash = Path().apply {
                moveTo(s * (3f / 18f), s * (3f / 18f))
                lineTo(s * (15f / 18f), s * (15f / 18f))
            }
            drawPath(slash, color = color, style = stroke)
            val frame = Path().apply {
                moveTo(s * (5.5f / 18f), s * (5.6f / 18f))
                cubicTo(s * (3.2f / 18f), s * (6.9f / 18f), s * (1.5f / 18f), s * (9f / 18f), s * (1.5f / 18f), s * (9f / 18f))
                cubicTo(s * (1.5f / 18f), s * (9f / 18f), s * (4f / 18f), s * (14f / 18f), s * (9f / 18f), s * (14f / 18f))
                cubicTo(s * (10.6f / 18f), s * (14f / 18f), s * (12f / 18f), s * (13.5f / 18f), s * (13.2f / 18f), s * (12.8f / 18f))
                moveTo(s * (8f / 18f), s * (4.1f / 18f))
                cubicTo(s * (8.3f / 18f), s * (4.1f / 18f), s * (8.7f / 18f), s * (4f / 18f), s * (9f / 18f), s * (4f / 18f))
                cubicTo(s * (14f / 18f), s * (4f / 18f), s * (16.5f / 18f), s * (9f / 18f), s * (16.5f / 18f), s * (9f / 18f))
                cubicTo(s * (16.5f / 18f), s * (9f / 18f), s * (15.6f / 18f), s * (10.8f / 18f), s * (13.9f / 18f), s * (12.2f / 18f))
                moveTo(s * (10.5f / 18f), s * (10.5f / 18f))
                cubicTo(s * (9.9f / 18f), s * (11f / 18f), s * (8.5f / 18f), s * (11f / 18f), s * (7.5f / 18f), s * (10.5f / 18f))
                cubicTo(s * (6.6f / 18f), s * (9.5f / 18f), s * (6.6f / 18f), s * (8.5f / 18f), s * (7.5f / 18f), s * (7.5f / 18f))
            }
            drawPath(frame, color = color, style = stroke)
        }
        SettingsGlyph.Copy -> {
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
        SettingsGlyph.Check -> {
            val heavy = Stroke(width = strokeWidth(s, base = 1.8f), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val path = Path().apply {
                moveTo(s * (3f / 18f), s * (9.5f / 18f))
                lineTo(s * (6.5f / 18f), s * (13f / 18f))
                lineTo(s * (15f / 18f), s * (5f / 18f))
            }
            drawPath(path, color = color, style = heavy)
        }
        SettingsGlyph.Shield -> {
            val shield = Path().apply {
                moveTo(s * (9f / 18f), s * (2.5f / 18f))
                lineTo(s * (15f / 18f), s * (4.5f / 18f))
                lineTo(s * (15f / 18f), s * (9.5f / 18f))
                cubicTo(s * (15f / 18f), s * (12.7f / 18f), s * (12.4f / 18f), s * (14.9f / 18f), s * (9f / 18f), s * (15.5f / 18f))
                cubicTo(s * (5.6f / 18f), s * (14.9f / 18f), s * (3f / 18f), s * (12.7f / 18f), s * (3f / 18f), s * (9.5f / 18f))
                lineTo(s * (3f / 18f), s * (4.5f / 18f))
                close()
            }
            drawPath(shield, color = color, style = stroke)
        }
        SettingsGlyph.Chevron -> {
            val path = Path().apply {
                moveTo(s * (7f / 18f), s * (4f / 18f))
                lineTo(s * (12f / 18f), s * (9f / 18f))
                lineTo(s * (7f / 18f), s * (14f / 18f))
            }
            drawPath(path, color = color, style = Stroke(width = strokeWidth(s, base = 1.6f), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        SettingsGlyph.ChevronLeft -> {
            val path = Path().apply {
                moveTo(s * (11f / 18f), s * (4f / 18f))
                lineTo(s * (6f / 18f), s * (9f / 18f))
                lineTo(s * (11f / 18f), s * (14f / 18f))
            }
            drawPath(path, color = color, style = Stroke(width = strokeWidth(s, base = 1.6f), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        SettingsGlyph.Megaphone -> {
            val cone = Path().apply {
                moveTo(s * (3f / 18f), s * (7f / 18f))
                lineTo(s * (3f / 18f), s * (11f / 18f))
                lineTo(s * (5f / 18f), s * (11f / 18f))
                lineTo(s * (12f / 18f), s * (14f / 18f))
                lineTo(s * (12f / 18f), s * (4f / 18f))
                lineTo(s * (5f / 18f), s * (7f / 18f))
                close()
            }
            drawPath(cone, color = color, style = stroke)
            val arc = Path().apply {
                moveTo(s * (12f / 18f), s * (6.5f / 18f))
                cubicTo(s * (14f / 18f), s * (7.5f / 18f), s * (14f / 18f), s * (10.5f / 18f), s * (12f / 18f), s * (11.5f / 18f))
            }
            drawPath(arc, color = color, style = stroke)
        }
        SettingsGlyph.Users -> {
            drawCircle(color = color, radius = s * (2.3f / 18f), center = Offset(s * (6.5f / 18f), s * (7f / 18f)), style = stroke)
            val left = Path().apply {
                moveTo(s * (2.5f / 18f), s * (14f / 18f))
                cubicTo(s * (3f / 18f), s * (11.8f / 18f), s * (4.6f / 18f), s * (10.5f / 18f), s * (6.5f / 18f), s * (10.5f / 18f))
                cubicTo(s * (8.4f / 18f), s * (10.5f / 18f), s * (10f / 18f), s * (11.8f / 18f), s * (10.5f / 18f), s * (14f / 18f))
            }
            drawPath(left, color = color, style = stroke)
            drawCircle(color = color, radius = s * (1.8f / 18f), center = Offset(s * (12.5f / 18f), s * (7.5f / 18f)), style = stroke)
            val right = Path().apply {
                moveTo(s * (12.5f / 18f), s * (10.5f / 18f))
                cubicTo(s * (14f / 18f), s * (10.5f / 18f), s * (15.2f / 18f), s * (11.5f / 18f), s * (15.5f / 18f), s * (13f / 18f))
            }
            drawPath(right, color = color, style = stroke)
        }
        SettingsGlyph.Envelope -> {
            val rect = Path().apply {
                moveTo(s * (2.5f / 18f), s * (4.5f / 18f))
                lineTo(s * (15.5f / 18f), s * (4.5f / 18f))
                lineTo(s * (15.5f / 18f), s * (13.5f / 18f))
                lineTo(s * (2.5f / 18f), s * (13.5f / 18f))
                close()
            }
            drawPath(rect, color = color, style = stroke)
            val flap = Path().apply {
                moveTo(s * (3f / 18f), s * (5.5f / 18f))
                lineTo(s * (9f / 18f), s * (9.5f / 18f))
                lineTo(s * (15f / 18f), s * (5.5f / 18f))
            }
            drawPath(flap, color = color, style = stroke)
        }
        SettingsGlyph.Sparkle -> {
            val path = Path().apply {
                moveTo(s * (9f / 18f), s * (2f / 18f))
                lineTo(s * (9f / 18f), s * (6f / 18f))
                moveTo(s * (9f / 18f), s * (12f / 18f))
                lineTo(s * (9f / 18f), s * (16f / 18f))
                moveTo(s * (2f / 18f), s * (9f / 18f))
                lineTo(s * (6f / 18f), s * (9f / 18f))
                moveTo(s * (12f / 18f), s * (9f / 18f))
                lineTo(s * (16f / 18f), s * (9f / 18f))
                moveTo(s * (4.5f / 18f), s * (4.5f / 18f))
                lineTo(s * (7f / 18f), s * (7f / 18f))
                moveTo(s * (11f / 18f), s * (11f / 18f))
                lineTo(s * (13.5f / 18f), s * (13.5f / 18f))
                moveTo(s * (4.5f / 18f), s * (13.5f / 18f))
                lineTo(s * (7f / 18f), s * (11f / 18f))
                moveTo(s * (11f / 18f), s * (7f / 18f))
                lineTo(s * (13.5f / 18f), s * (4.5f / 18f))
            }
            drawPath(path, color = color, style = stroke)
        }
        SettingsGlyph.Pencil -> {
            val path = Path().apply {
                moveTo(s * (3f / 18f), s * (15f / 18f))
                lineTo(s * (4f / 18f), s * (12f / 18f))
                lineTo(s * (12f / 18f), s * (4f / 18f))
                lineTo(s * (14f / 18f), s * (6f / 18f))
                lineTo(s * (6f / 18f), s * (14f / 18f))
                close()
                moveTo(s * (11f / 18f), s * (5f / 18f))
                lineTo(s * (13f / 18f), s * (7f / 18f))
            }
            drawPath(path, color = color, style = stroke)
        }
        SettingsGlyph.Calendar -> {
            val rect = Path().apply {
                moveTo(s * (2.5f / 18f), s * (4f / 18f))
                lineTo(s * (15.5f / 18f), s * (4f / 18f))
                lineTo(s * (15.5f / 18f), s * (15f / 18f))
                lineTo(s * (2.5f / 18f), s * (15f / 18f))
                close()
            }
            drawPath(rect, color = color, style = stroke)
            val ticks = Path().apply {
                moveTo(s * (2.5f / 18f), s * (7.5f / 18f))
                lineTo(s * (15.5f / 18f), s * (7.5f / 18f))
                moveTo(s * (6f / 18f), s * (2.5f / 18f))
                lineTo(s * (6f / 18f), s * (5.5f / 18f))
                moveTo(s * (12f / 18f), s * (2.5f / 18f))
                lineTo(s * (12f / 18f), s * (5.5f / 18f))
            }
            drawPath(ticks, color = color, style = stroke)
        }
        SettingsGlyph.Pin -> {
            val pin = Path().apply {
                moveTo(s * (9f / 18f), s * (2f / 18f))
                cubicTo(s * (11.5f / 18f), s * (2f / 18f), s * (13.5f / 18f), s * (4f / 18f), s * (13.5f / 18f), s * (6.5f / 18f))
                cubicTo(s * (13.5f / 18f), s * (9.5f / 18f), s * (9f / 18f), s * (15.5f / 18f), s * (9f / 18f), s * (15.5f / 18f))
                cubicTo(s * (9f / 18f), s * (15.5f / 18f), s * (4.5f / 18f), s * (9.5f / 18f), s * (4.5f / 18f), s * (6.5f / 18f))
                cubicTo(s * (4.5f / 18f), s * (4f / 18f), s * (6.5f / 18f), s * (2f / 18f), s * (9f / 18f), s * (2f / 18f))
                close()
            }
            drawPath(pin, color = color, style = stroke)
            drawCircle(color = color, radius = s * (1.5f / 18f), center = Offset(s * (9f / 18f), s * (6.5f / 18f)), style = stroke)
        }
        SettingsGlyph.Book -> {
            val path = Path().apply {
                moveTo(s * (3f / 18f), s * (3.5f / 18f))
                lineTo(s * (8.5f / 18f), s * (3.5f / 18f))
                cubicTo(s * (9.5f / 18f), s * (3.5f / 18f), s * (10f / 18f), s * (4f / 18f), s * (10f / 18f), s * (5f / 18f))
                lineTo(s * (10f / 18f), s * (15f / 18f))
                cubicTo(s * (10f / 18f), s * (14f / 18f), s * (9.5f / 18f), s * (13.5f / 18f), s * (8.5f / 18f), s * (13.5f / 18f))
                lineTo(s * (3f / 18f), s * (13.5f / 18f))
                close()
                moveTo(s * (15f / 18f), s * (3.5f / 18f))
                lineTo(s * (9.5f / 18f), s * (3.5f / 18f))
                cubicTo(s * (8.5f / 18f), s * (3.5f / 18f), s * (8f / 18f), s * (4f / 18f), s * (8f / 18f), s * (5f / 18f))
                lineTo(s * (8f / 18f), s * (15f / 18f))
                cubicTo(s * (8f / 18f), s * (14f / 18f), s * (8.5f / 18f), s * (13.5f / 18f), s * (9.5f / 18f), s * (13.5f / 18f))
                lineTo(s * (15f / 18f), s * (13.5f / 18f))
                close()
            }
            drawPath(path, color = color, style = stroke)
        }
        SettingsGlyph.Tag -> {
            val tag = Path().apply {
                moveTo(s * (2.5f / 18f), s * (2.5f / 18f))
                lineTo(s * (7.5f / 18f), s * (2.5f / 18f))
                lineTo(s * (15.5f / 18f), s * (10.5f / 18f))
                lineTo(s * (10.5f / 18f), s * (15.5f / 18f))
                lineTo(s * (2.5f / 18f), s * (7.5f / 18f))
                close()
            }
            drawPath(tag, color = color, style = stroke)
            drawCircle(color = color, radius = s * (1f / 18f), center = Offset(s * (5.5f / 18f), s * (5.5f / 18f)), style = stroke)
        }
    }
}

private fun strokeWidth(canvasSize: Float, base: Float = 1.4f): Float =
    base * (canvasSize / 18f).coerceAtLeast(0.6f)
