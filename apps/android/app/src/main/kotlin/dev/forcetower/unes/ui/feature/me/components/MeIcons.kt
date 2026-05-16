package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.forcetower.unes.ui.feature.me.SettingsIcon
import dev.forcetower.unes.ui.feature.me.ShortcutIcon

// Stroke-based icons used by the Me screen. Mirrors the `MeIcon` switch in
// `screens-me.jsx` — uniform stroke width, rounded caps, square 18-unit
// viewbox per glyph, drawn as fractions of the canvas size so the same path
// scales cleanly across the 16dp shortcut badges and the 18dp settings rows.
//
// We keep the icons inside the Me feature instead of upgrading the design
// system because this set is intentionally one-off: the JSX prototype draws
// its own glyphs to keep the Me hub visually distinct from the Material
// catalogue used elsewhere in the app.

@Composable
internal fun MeShortcutIconBox(icon: ShortcutIcon, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawShortcutIcon(icon, color, this.size.minDimension)
    }
}

@Composable
internal fun MeSettingsIconBox(icon: SettingsIcon, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawSettingsIcon(icon, color, this.size.minDimension)
    }
}

@Composable
internal fun MeChevronGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = this.size.minDimension
        val stroke = Stroke(width = strokeWidth(s), cap = StrokeCap.Round, join = StrokeJoin.Round)
        // 18-unit viewbox: M7 4 l5 5 -5 5
        val path = Path().apply {
            moveTo(s * (7f / 18f), s * (4f / 18f))
            lineTo(s * (12f / 18f), s * (9f / 18f))
            lineTo(s * (7f / 18f), s * (14f / 18f))
        }
        drawPath(path, color = color, style = stroke)
    }
}

@Composable
internal fun MeQrGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = this.size.minDimension
        val stroke = Stroke(width = strokeWidth(s), cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun rect(x: Float, y: Float, w: Float, h: Float) {
            val path = Path().apply {
                moveTo(s * x, s * y)
                lineTo(s * (x + w), s * y)
                lineTo(s * (x + w), s * (y + h))
                lineTo(s * x, s * (y + h))
                close()
            }
            drawPath(path, color = color, style = stroke)
        }
        // Three corner squares (top-left, top-right, bottom-left).
        rect(2.5f / 18f, 2.5f / 18f, 4.5f / 18f, 4.5f / 18f)
        rect(11f / 18f, 2.5f / 18f, 4.5f / 18f, 4.5f / 18f)
        rect(2.5f / 18f, 11f / 18f, 4.5f / 18f, 4.5f / 18f)
        // Loose dots on the bottom-right module (matches JSX `M11 11h1.5v1.5...`).
        val tail = Path().apply {
            moveTo(s * (11f / 18f), s * (11f / 18f))
            lineTo(s * (12.5f / 18f), s * (11f / 18f))
            lineTo(s * (12.5f / 18f), s * (12.5f / 18f))
            moveTo(s * (15.5f / 18f), s * (11f / 18f))
            lineTo(s * (15.5f / 18f), s * (12.5f / 18f))
            moveTo(s * (11f / 18f), s * (15.5f / 18f))
            lineTo(s * (12.5f / 18f), s * (15.5f / 18f))
            moveTo(s * (14f / 18f), s * (13.5f / 18f))
            lineTo(s * (15.5f / 18f), s * (13.5f / 18f))
            lineTo(s * (15.5f / 18f), s * (15.5f / 18f))
        }
        drawPath(tail, color = color, style = stroke)
    }
}

@Composable
internal fun MeExitGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = this.size.minDimension
        val stroke = Stroke(width = strokeWidth(s), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val frame = Path().apply {
            // Door rectangle on the right
            moveTo(s * (11f / 18f), s * (3f / 18f))
            lineTo(s * (13.5f / 18f), s * (3f / 18f))
            lineTo(s * (14.5f / 18f), s * (4f / 18f))
            lineTo(s * (14.5f / 18f), s * (14f / 18f))
            lineTo(s * (13.5f / 18f), s * (15f / 18f))
            lineTo(s * (11f / 18f), s * (15f / 18f))
            // Arrow shaft + head
            moveTo(s * (8f / 18f), s * (6f / 18f))
            lineTo(s * (5f / 18f), s * (9f / 18f))
            lineTo(s * (8f / 18f), s * (12f / 18f))
            moveTo(s * (5f / 18f), s * (9f / 18f))
            lineTo(s * (12f / 18f), s * (9f / 18f))
        }
        drawPath(frame, color = color, style = stroke)
    }
}

@Composable
internal fun MeArrowRightGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = this.size.minDimension
        val stroke = Stroke(width = strokeWidth(s, base = 1.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(s * 0.25f, s * 0.5f)
            lineTo(s * 0.75f, s * 0.5f)
            moveTo(s * 0.5f, s * 0.25f)
            lineTo(s * 0.75f, s * 0.5f)
            lineTo(s * 0.5f, s * 0.75f)
        }
        drawPath(path, color = color, style = stroke)
    }
}

@Composable
internal fun MeCloseGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = this.size.minDimension
        val stroke = Stroke(width = strokeWidth(s, base = 1.6f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(s * (2f / 11f), s * (2f / 11f))
            lineTo(s * (9f / 11f), s * (9f / 11f))
            moveTo(s * (9f / 11f), s * (2f / 11f))
            lineTo(s * (2f / 11f), s * (9f / 11f))
        }
        drawPath(path, color = color, style = stroke)
    }
}

@Composable
internal fun MeCheckGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = this.size.minDimension
        val stroke = Stroke(width = strokeWidth(s, base = 1.7f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(s * (2f / 13f), s * (6.5f / 13f))
            lineTo(s * (5f / 13f), s * (9.5f / 13f))
            lineTo(s * (10f / 13f), s * (3.5f / 13f))
        }
        drawPath(path, color = color, style = stroke)
    }
}

@Composable
internal fun MeCopyGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = this.size.minDimension
        val stroke = Stroke(width = strokeWidth(s, base = 1.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val rect = Path().apply {
            moveTo(s * (3.5f / 13f), s * (3.5f / 13f))
            lineTo(s * (10.5f / 13f), s * (3.5f / 13f))
            lineTo(s * (10.5f / 13f), s * (11.5f / 13f))
            lineTo(s * (3.5f / 13f), s * (11.5f / 13f))
            close()
        }
        drawPath(rect, color = color, style = stroke)
        val flap = Path().apply {
            moveTo(s * (5.5f / 13f), s * (3.5f / 13f))
            lineTo(s * (5.5f / 13f), s * (2.2f / 13f))
            lineTo(s * (10.3f / 13f), s * (2.2f / 13f))
            lineTo(s * (11f / 13f), s * (2.9f / 13f))
            lineTo(s * (11f / 13f), s * (9f / 13f))
            lineTo(s * (10.3f / 13f), s * (9.7f / 13f))
            lineTo(s * (10f / 13f), s * (9.7f / 13f))
        }
        drawPath(flap, color = color, style = stroke)
    }
}

private fun strokeWidth(canvasSize: Float, base: Float = 1.4f): Float =
    base * (canvasSize / 18f).coerceAtLeast(0.6f)

private fun DrawScope.drawShortcutIcon(icon: ShortcutIcon, color: Color, size: Float) {
    val stroke = Stroke(width = strokeWidth(size), cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (icon) {
        ShortcutIcon.Account -> {
            // 18-unit head circle + shoulders curve.
            drawCircle(color = color, radius = size * 0.18f, center = androidx.compose.ui.geometry.Offset(size / 2f, size * 0.42f), style = stroke)
            val path = Path().apply {
                moveTo(size * 0.22f, size * 0.85f)
                cubicTo(
                    size * 0.30f, size * 0.65f,
                    size * 0.46f, size * 0.60f,
                    size * 0.50f, size * 0.60f,
                )
                cubicTo(
                    size * 0.54f, size * 0.60f,
                    size * 0.70f, size * 0.65f,
                    size * 0.78f, size * 0.85f,
                )
            }
            drawPath(path, color = color, style = stroke)
        }
        ShortcutIcon.Hourglass -> {
            val path = Path().apply {
                moveTo(size * (5f / 18f), size * (3f / 18f))
                lineTo(size * (13f / 18f), size * (3f / 18f))
                moveTo(size * (5f / 18f), size * (15f / 18f))
                lineTo(size * (13f / 18f), size * (15f / 18f))
                moveTo(size * (6f / 18f), size * (3f / 18f))
                cubicTo(
                    size * (6f / 18f), size * (6f / 18f),
                    size * (12f / 18f), size * (6f / 18f),
                    size * (12f / 18f), size * (9f / 18f),
                )
                cubicTo(
                    size * (12f / 18f), size * (12f / 18f),
                    size * (6f / 18f), size * (12f / 18f),
                    size * (6f / 18f), size * (15f / 18f),
                )
                moveTo(size * (12f / 18f), size * (3f / 18f))
                cubicTo(
                    size * (12f / 18f), size * (6f / 18f),
                    size * (6f / 18f), size * (6f / 18f),
                    size * (6f / 18f), size * (9f / 18f),
                )
                cubicTo(
                    size * (6f / 18f), size * (12f / 18f),
                    size * (12f / 18f), size * (12f / 18f),
                    size * (12f / 18f), size * (15f / 18f),
                )
            }
            drawPath(path, color = color, style = stroke)
        }
        ShortcutIcon.Flow -> {
            // Three rounded rects + connectors.
            roundedRect(size, color, stroke, 6.5f, 2.5f, 5f, 3.5f, 0.8f)
            roundedRect(size, color, stroke, 2.5f, 11.5f, 4f, 3.5f, 0.8f)
            roundedRect(size, color, stroke, 11.5f, 11.5f, 4f, 3.5f, 0.8f)
            val connectors = Path().apply {
                moveTo(size * (9f / 18f), size * (6f / 18f))
                lineTo(size * (9f / 18f), size * (8.5f / 18f))
                moveTo(size * (9f / 18f), size * (8.5f / 18f))
                lineTo(size * (4.5f / 18f), size * (8.5f / 18f))
                lineTo(size * (4.5f / 18f), size * (11.5f / 18f))
                moveTo(size * (9f / 18f), size * (8.5f / 18f))
                lineTo(size * (13.5f / 18f), size * (8.5f / 18f))
                lineTo(size * (13.5f / 18f), size * (11.5f / 18f))
            }
            drawPath(connectors, color = color, style = stroke)
        }
        ShortcutIcon.Tray -> {
            val path = Path().apply {
                moveTo(size * (2.5f / 18f), size * (11.5f / 18f))
                cubicTo(
                    size * (2.5f / 18f), size * (13f / 18f),
                    size * (5.4f / 18f), size * (14f / 18f),
                    size * (9f / 18f), size * (14f / 18f),
                )
                cubicTo(
                    size * (12.6f / 18f), size * (14f / 18f),
                    size * (15.5f / 18f), size * (13f / 18f),
                    size * (15.5f / 18f), size * (11.5f / 18f),
                )
                moveTo(size * (4f / 18f), size * (11f / 18f))
                lineTo(size * (5f / 18f), size * (6.5f / 18f))
                lineTo(size * (7f / 18f), size * (5f / 18f))
                lineTo(size * (11f / 18f), size * (5f / 18f))
                lineTo(size * (13f / 18f), size * (6.5f / 18f))
                lineTo(size * (14f / 18f), size * (11f / 18f))
                moveTo(size * (7f / 18f), size * (5.5f / 18f))
                lineTo(size * (7f / 18f), size * (4f / 18f))
                moveTo(size * (11f / 18f), size * (5.5f / 18f))
                lineTo(size * (11f / 18f), size * (4f / 18f))
            }
            drawPath(path, color = color, style = stroke)
        }
        ShortcutIcon.Calendar -> {
            roundedRect(size, color, stroke, 2.5f, 4f, 13f, 11f, 1.6f)
            val path = Path().apply {
                moveTo(size * (2.5f / 18f), size * (7.5f / 18f))
                lineTo(size * (15.5f / 18f), size * (7.5f / 18f))
                moveTo(size * (6f / 18f), size * (2.5f / 18f))
                lineTo(size * (6f / 18f), size * (5.5f / 18f))
                moveTo(size * (12f / 18f), size * (2.5f / 18f))
                lineTo(size * (12f / 18f), size * (5.5f / 18f))
            }
            drawPath(path, color = color, style = stroke)
            // Filled marker dot.
            drawCircle(color = color, radius = size * (0.8f / 18f), center = androidx.compose.ui.geometry.Offset(size * (6f / 18f), size * (11f / 18f)))
        }
        ShortcutIcon.Timer -> {
            drawCircle(color = color, radius = size * (6f / 18f), center = androidx.compose.ui.geometry.Offset(size * (9f / 18f), size * (10f / 18f)), style = stroke)
            val path = Path().apply {
                moveTo(size * (9f / 18f), size * (10f / 18f))
                lineTo(size * (9f / 18f), size * (6.5f / 18f))
                moveTo(size * (7f / 18f), size * (2.5f / 18f))
                lineTo(size * (11f / 18f), size * (2.5f / 18f))
                moveTo(size * (9f / 18f), size * (2.5f / 18f))
                lineTo(size * (9f / 18f), size * (4f / 18f))
            }
            drawPath(path, color = color, style = stroke)
        }
        ShortcutIcon.Doc -> {
            val outline = Path().apply {
                moveTo(size * (4f / 18f), size * (2.5f / 18f))
                lineTo(size * (10f / 18f), size * (2.5f / 18f))
                lineTo(size * (13.5f / 18f), size * (6f / 18f))
                lineTo(size * (13.5f / 18f), size * (15f / 18f))
                lineTo(size * (12.5f / 18f), size * (16f / 18f))
                lineTo(size * (4f / 18f), size * (16f / 18f))
                lineTo(size * (3f / 18f), size * (15f / 18f))
                lineTo(size * (3f / 18f), size * (3.5f / 18f))
                close()
            }
            drawPath(outline, color = color, style = stroke)
            val lines = Path().apply {
                moveTo(size * (10f / 18f), size * (2.5f / 18f))
                lineTo(size * (10f / 18f), size * (6f / 18f))
                lineTo(size * (13.5f / 18f), size * (6f / 18f))
                moveTo(size * (6f / 18f), size * (9.5f / 18f))
                lineTo(size * (12f / 18f), size * (9.5f / 18f))
                moveTo(size * (6f / 18f), size * (12f / 18f))
                lineTo(size * (12f / 18f), size * (12f / 18f))
                moveTo(size * (6f / 18f), size * (14f / 18f))
                lineTo(size * (10f / 18f), size * (14f / 18f))
            }
            drawPath(lines, color = color, style = stroke)
        }
        ShortcutIcon.Brush -> {
            val path = Path().apply {
                moveTo(size * (11f / 18f), size * (3f / 18f))
                lineTo(size * (15f / 18f), size * (7f / 18f))
                lineTo(size * (7.5f / 18f), size * (14.5f / 18f))
                cubicTo(
                    size * (6.7f / 18f), size * (15.3f / 18f),
                    size * (5.5f / 18f), size * (15.3f / 18f),
                    size * (4.7f / 18f), size * (14.5f / 18f),
                )
                lineTo(size * (3.5f / 18f), size * (13.3f / 18f))
                cubicTo(
                    size * (2.7f / 18f), size * (12.5f / 18f),
                    size * (2.7f / 18f), size * (11.3f / 18f),
                    size * (3.5f / 18f), size * (10.5f / 18f),
                )
                close()
                moveTo(size * (10f / 18f), size * (4f / 18f))
                lineTo(size * (14f / 18f), size * (8f / 18f))
            }
            drawPath(path, color = color, style = stroke)
        }
        ShortcutIcon.Bell -> {
            val path = Path().apply {
                moveTo(size * (4.5f / 18f), size * (12.5f / 18f))
                lineTo(size * (13.5f / 18f), size * (12.5f / 18f))
                lineTo(size * (12.5f / 18f), size * (10.5f / 18f))
                lineTo(size * (12.5f / 18f), size * (7.5f / 18f))
                cubicTo(
                    size * (12.5f / 18f), size * (5.5f / 18f),
                    size * (10.7f / 18f), size * (4f / 18f),
                    size * (9f / 18f), size * (4f / 18f),
                )
                cubicTo(
                    size * (7.3f / 18f), size * (4f / 18f),
                    size * (5.5f / 18f), size * (5.5f / 18f),
                    size * (5.5f / 18f), size * (7.5f / 18f),
                )
                lineTo(size * (5.5f / 18f), size * (10.5f / 18f))
                close()
                moveTo(size * (7.5f / 18f), size * (14.5f / 18f))
                cubicTo(
                    size * (7.7f / 18f), size * (15.3f / 18f),
                    size * (10.3f / 18f), size * (15.3f / 18f),
                    size * (10.5f / 18f), size * (14.5f / 18f),
                )
            }
            drawPath(path, color = color, style = stroke)
        }
        ShortcutIcon.Compass -> {
            drawCircle(color = color, radius = size * (6.5f / 18f), center = androidx.compose.ui.geometry.Offset(size / 2f, size / 2f), style = stroke)
            val needle = Path().apply {
                fillType = PathFillType.EvenOdd
                moveTo(size * (11.5f / 18f), size * (6.5f / 18f))
                lineTo(size * (10.5f / 18f), size * (9.5f / 18f))
                lineTo(size * (7.5f / 18f), size * (10.5f / 18f))
                lineTo(size * (8.5f / 18f), size * (7.5f / 18f))
                close()
            }
            drawPath(needle, color = color, style = stroke)
        }
    }
}

private fun DrawScope.drawSettingsIcon(icon: SettingsIcon, color: Color, size: Float) {
    val stroke = Stroke(width = strokeWidth(size), cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (icon) {
        SettingsIcon.Gear -> {
            drawCircle(color = color, radius = size * (2.2f / 18f), center = androidx.compose.ui.geometry.Offset(size / 2f, size / 2f), style = stroke)
            val arms = Path().apply {
                moveTo(size * (9f / 18f), size * (2f / 18f))
                lineTo(size * (9f / 18f), size * (4f / 18f))
                moveTo(size * (9f / 18f), size * (14f / 18f))
                lineTo(size * (9f / 18f), size * (16f / 18f))
                moveTo(size * (2f / 18f), size * (9f / 18f))
                lineTo(size * (4f / 18f), size * (9f / 18f))
                moveTo(size * (14f / 18f), size * (9f / 18f))
                lineTo(size * (16f / 18f), size * (9f / 18f))
                moveTo(size * (4f / 18f), size * (4f / 18f))
                lineTo(size * (5.4f / 18f), size * (5.4f / 18f))
                moveTo(size * (12.6f / 18f), size * (12.6f / 18f))
                lineTo(size * (14f / 18f), size * (14f / 18f))
                moveTo(size * (4f / 18f), size * (14f / 18f))
                lineTo(size * (5.4f / 18f), size * (12.6f / 18f))
                moveTo(size * (12.6f / 18f), size * (5.4f / 18f))
                lineTo(size * (14f / 18f), size * (4f / 18f))
            }
            drawPath(arms, color = color, style = stroke)
        }
        SettingsIcon.Info -> {
            drawCircle(color = color, radius = size * (6.5f / 18f), center = androidx.compose.ui.geometry.Offset(size / 2f, size / 2f), style = stroke)
            val body = Path().apply {
                moveTo(size * (9f / 18f), size * (8.5f / 18f))
                lineTo(size * (9f / 18f), size * (12.5f / 18f))
                moveTo(size * (9f / 18f), size * (6f / 18f))
                lineTo(size * (9f / 18f), size * (6.3f / 18f))
            }
            drawPath(body, color = color, style = stroke)
        }
        SettingsIcon.Bug -> {
            // Body ellipse + legs + antennae.
            val body = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        left = size * (5.5f / 18f),
                        top = size * (6f / 18f),
                        right = size * (12.5f / 18f),
                        bottom = size * (14f / 18f),
                    ),
                )
            }
            drawPath(body, color = color, style = stroke)
            val legs = Path().apply {
                moveTo(size * (5.5f / 18f), size * (10f / 18f))
                lineTo(size * (3.5f / 18f), size * (10f / 18f))
                moveTo(size * (12.5f / 18f), size * (10f / 18f))
                lineTo(size * (14.5f / 18f), size * (10f / 18f))
                moveTo(size * (6f / 18f), size * (7f / 18f))
                lineTo(size * (4.5f / 18f), size * (5.5f / 18f))
                moveTo(size * (12f / 18f), size * (7f / 18f))
                lineTo(size * (13.5f / 18f), size * (5.5f / 18f))
                moveTo(size * (6f / 18f), size * (13f / 18f))
                lineTo(size * (4.5f / 18f), size * (14.5f / 18f))
                moveTo(size * (12f / 18f), size * (13f / 18f))
                lineTo(size * (13.5f / 18f), size * (14.5f / 18f))
                moveTo(size * (9f / 18f), size * (6.5f / 18f))
                lineTo(size * (9f / 18f), size * (4.5f / 18f))
                moveTo(size * (7f / 18f), size * (4.5f / 18f))
                cubicTo(
                    size * (7f / 18f), size * (3.4f / 18f),
                    size * (11f / 18f), size * (3.4f / 18f),
                    size * (11f / 18f), size * (4.5f / 18f),
                )
            }
            drawPath(legs, color = color, style = stroke)
        }
        SettingsIcon.License -> {
            drawCircle(color = color, radius = size * (6.5f / 18f), center = androidx.compose.ui.geometry.Offset(size / 2f, size / 2f), style = stroke)
            val c = Path().apply {
                moveTo(size * (11.5f / 18f), size * (7f / 18f))
                cubicTo(
                    size * (11f / 18f), size * (6.2f / 18f),
                    size * (10f / 18f), size * (5.7f / 18f),
                    size * (9f / 18f), size * (5.7f / 18f),
                )
                cubicTo(
                    size * (7.3f / 18f), size * (5.7f / 18f),
                    size * (6f / 18f), size * (7.2f / 18f),
                    size * (6f / 18f), size * (9f / 18f),
                )
                cubicTo(
                    size * (6f / 18f), size * (10.8f / 18f),
                    size * (7.3f / 18f), size * (12.3f / 18f),
                    size * (9f / 18f), size * (12.3f / 18f),
                )
                cubicTo(
                    size * (10f / 18f), size * (12.3f / 18f),
                    size * (11f / 18f), size * (11.8f / 18f),
                    size * (11.5f / 18f), size * (11f / 18f),
                )
            }
            drawPath(c, color = color, style = stroke)
        }
    }
}

private fun DrawScope.roundedRect(
    canvas: Float,
    color: Color,
    stroke: Stroke,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    @Suppress("UNUSED_PARAMETER") r: Float,
) {
    // The original viewbox uses 18-unit coordinates with small corner radii;
    // converting to canvas pixels and using a Path approximates the JSX rect
    // closely enough — exact pixel-radius isn't visible at 16-30dp icon sizes.
    val path = Path().apply {
        moveTo(canvas * (x / 18f), canvas * (y / 18f))
        lineTo(canvas * ((x + w) / 18f), canvas * (y / 18f))
        lineTo(canvas * ((x + w) / 18f), canvas * ((y + h) / 18f))
        lineTo(canvas * (x / 18f), canvas * ((y + h) / 18f))
        close()
    }
    drawPath(path, color = color, style = stroke)
}
