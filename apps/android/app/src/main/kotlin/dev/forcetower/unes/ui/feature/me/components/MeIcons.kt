package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

// Stroke-based glyphs still used by the logout sheet and logout flash. The
// main screen and the about/document sheets moved to Material icons with the
// 2026 Eu redesign; these stay because the logout surfaces draw them at
// sizes and stroke weights the Material set doesn't match.

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

private fun strokeWidth(canvasSize: Float, base: Float = 1.4f): Float =
    base * (canvasSize / 18f).coerceAtLeast(0.6f)
