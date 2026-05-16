package dev.forcetower.unes.ui.feature.finalcountdown.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.forcetower.unes.ui.feature.finalcountdown.FCVerdictIcon

// Stroke-based glyph set for the verdict eyebrow chip. Same shapes the JSX
// prototype draws in `FCIcon` (`screens-final-countdown.jsx`). Each path is
// authored against an 18×18 viewBox and rescaled to the Canvas size.

@Composable
internal fun FCVerdictGlyph(
    icon: FCVerdictIcon,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 1.6f,
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 18f
        val stroke = Stroke(
            width = strokeWidth * s,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        when (icon) {
            FCVerdictIcon.Trophy -> {
                val path = Path().apply {
                    moveTo(5f * s, 3f * s)
                    lineTo(13f * s, 3f * s)
                    lineTo(13f * s, 6.5f * s)
                    relativeCubicTo(0f, 2.2f * s, -1.8f * s, 4f * s, -4f * s, 4f * s)
                    relativeCubicTo(-2.2f * s, 0f, -4f * s, -1.8f * s, -4f * s, -4f * s)
                    lineTo(5f * s, 3f * s)
                    close()

                    moveTo(5f * s, 4f * s)
                    lineTo(3f * s, 4f * s)
                    lineTo(3f * s, 5.5f * s)
                    relativeCubicTo(0f, 1f * s, 0.7f * s, 1.8f * s, 2f * s, 2f * s)

                    moveTo(13f * s, 4f * s)
                    lineTo(15f * s, 4f * s)
                    lineTo(15f * s, 5.5f * s)
                    relativeCubicTo(0f, 1f * s, -0.7f * s, 1.8f * s, -2f * s, 2f * s)

                    moveTo(7f * s, 11f * s)
                    lineTo(11f * s, 11f * s)
                    lineTo(11f * s, 13f * s)
                    lineTo(12.5f * s, 15f * s)
                    lineTo(5.5f * s, 15f * s)
                    lineTo(7f * s, 13f * s)
                    close()
                }
                drawPath(path = path, color = color, style = stroke)
            }

            FCVerdictIcon.Check -> {
                val path = Path().apply {
                    moveTo(3f * s, 9.5f * s)
                    lineTo(6.5f * s, 13f * s)
                    lineTo(15f * s, 5f * s)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = 2f * s,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }

            FCVerdictIcon.Bolt -> {
                val path = Path().apply {
                    moveTo(10f * s, 2f * s)
                    lineTo(3f * s, 10f * s)
                    lineTo(8f * s, 10f * s)
                    lineTo(7f * s, 16f * s)
                    lineTo(14f * s, 8f * s)
                    lineTo(9f * s, 8f * s)
                    lineTo(10f * s, 2f * s)
                    close()
                }
                drawPath(path = path, color = color, style = stroke)
            }

            FCVerdictIcon.Flag -> {
                val path = Path().apply {
                    moveTo(4f * s, 2f * s)
                    lineTo(4f * s, 16f * s)

                    moveTo(4f * s, 3f * s)
                    lineTo(13f * s, 3f * s)
                    lineTo(11f * s, 6f * s)
                    lineTo(13f * s, 9f * s)
                    lineTo(4f * s, 9f * s)
                }
                drawPath(path = path, color = color, style = stroke)
            }

            FCVerdictIcon.Skull -> {
                val path = Path().apply {
                    moveTo(9f * s, 2f * s)
                    relativeCubicTo(-3.3f * s, 0f, -6f * s, 2.5f * s, -6f * s, 5.8f * s)
                    relativeCubicTo(0f, 1.7f * s, 0.7f * s, 3.2f * s, 1.8f * s, 4.2f * s)
                    lineTo(5f * s, 14.5f * s)
                    lineTo(7f * s, 14.5f * s)
                    lineTo(7f * s, 12.5f * s)
                    lineTo(8f * s, 12.5f * s)
                    lineTo(8f * s, 14.5f * s)
                    lineTo(10f * s, 14.5f * s)
                    lineTo(10f * s, 12.5f * s)
                    lineTo(11f * s, 12.5f * s)
                    lineTo(11f * s, 14.5f * s)
                    lineTo(13f * s, 14.5f * s)
                    lineTo(14.2f * s, 12f * s)
                    relativeCubicTo(1.1f * s, -1f * s, 1.8f * s, -2.5f * s, 1.8f * s, -4.2f * s)
                    relativeCubicTo(0f, -3.3f * s, -2.7f * s, -5.8f * s, -7f * s, -5.8f * s)
                    close()
                }
                drawPath(path = path, color = color, style = stroke)
                // Eye sockets (filled discs).
                drawCircle(color = color, radius = 1f * s, center = Offset(6.5f * s, 8f * s))
                drawCircle(color = color, radius = 1f * s, center = Offset(11.5f * s, 8f * s))
            }

            FCVerdictIcon.Sparkle -> {
                val path = Path().apply {
                    moveTo(9f * s, 2f * s); lineTo(9f * s, 6f * s)
                    moveTo(9f * s, 12f * s); lineTo(9f * s, 16f * s)
                    moveTo(2f * s, 9f * s); lineTo(6f * s, 9f * s)
                    moveTo(12f * s, 9f * s); lineTo(16f * s, 9f * s)
                    moveTo(4.5f * s, 4.5f * s); lineTo(7f * s, 7f * s)
                    moveTo(11f * s, 11f * s); lineTo(13.5f * s, 13.5f * s)
                    moveTo(4.5f * s, 13.5f * s); lineTo(7f * s, 11f * s)
                    moveTo(11f * s, 7f * s); lineTo(13.5f * s, 4.5f * s)
                }
                drawPath(path = path, color = color, style = stroke)
            }
        }
    }
}

// Star glyph used by wildcard rows (filled "★"). Drawn separately because the
// breakdown component needs it as a small inline decoration.
@Composable
internal fun FCStar(
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 18f
        val path = Path().apply {
            moveTo(9f * s, 2f * s)
            lineTo(11f * s, 6.5f * s)
            lineTo(16f * s, 7.1f * s)
            lineTo(12.3f * s, 10.5f * s)
            lineTo(13.3f * s, 15.4f * s)
            lineTo(9f * s, 13f * s)
            lineTo(4.7f * s, 15.4f * s)
            lineTo(5.7f * s, 10.5f * s)
            lineTo(2f * s, 7.1f * s)
            lineTo(7f * s, 6.5f * s)
            close()
        }
        if (filled) {
            drawPath(path = path, color = color)
        } else {
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 1.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
