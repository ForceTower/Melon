package dev.forcetower.unes.ui.feature.messages.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.ui.feature.messages.Message
import dev.forcetower.unes.ui.feature.messages.MessageOrigin
import dev.forcetower.unes.ui.feature.messages.MessageOriginMeta
import dev.forcetower.unes.ui.feature.messages.originMeta

// Origin swatch shown at the left of every message row and on the detail
// sender card. Shape and glyph depend on `message.origin`.
//
// * `Direct`     — circle with a tinted ring + person glyph.
// * `Discipline` — rounded rect in the discipline accent with the short code.
// * everything else — rounded tile with a tinted background and a kind icon.
@Composable
internal fun OriginSwatch(
    message: Message,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val meta = originMeta(message)
    when (message.origin) {
        MessageOrigin.Direct -> DirectSwatch(meta, size, modifier)
        MessageOrigin.Discipline -> DisciplineSwatch(meta, size, modifier)
        else -> IconSwatch(message.origin, meta, size, modifier)
    }
}

@Composable
private fun DirectSwatch(meta: MessageOriginMeta, size: Dp, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(meta.color.copy(alpha = 0.09f))
            .border(1.5.dp, meta.color.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size * 0.38f)) {
            val w = this.size.width
            val h = this.size.height
            val sx = w / 15f
            val sy = h / 15f
            val stroke = Stroke(width = 1.3f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
            // head: circle radius 2.3 around (7.5, 5)
            drawCircle(
                color = meta.color,
                radius = 2.3f * sx,
                center = androidx.compose.ui.geometry.Offset(7.5f * sx, 5f * sy),
                style = stroke,
            )
            // shoulders curve
            val path = Path().apply {
                moveTo(2.5f * sx, 13f * sy)
                quadraticTo(7.5f * sx, 7.3f * sy, 12.5f * sx, 13f * sy)
            }
            drawPath(path = path, color = meta.color, style = stroke)
        }
    }
}

@Composable
private fun DisciplineSwatch(meta: MessageOriginMeta, size: Dp, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(11.dp))
            .background(meta.color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = meta.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun IconSwatch(origin: MessageOrigin, meta: MessageOriginMeta, size: Dp, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(11.dp))
            .background(meta.color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        OriginGlyph(origin = origin, color = meta.color, size = 18.dp)
    }
}

@Composable
private fun OriginGlyph(origin: MessageOrigin, color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sx = w / 18f
        val sy = h / 18f
        when (origin) {
            MessageOrigin.App -> {
                // chat card: M4 4h10v7l-4-2-4 3V4z
                val path = Path().apply {
                    moveTo(4f * sx, 4f * sy)
                    lineTo(14f * sx, 4f * sy)
                    lineTo(14f * sx, 11f * sy)
                    lineTo(10f * sx, 9f * sy)
                    lineTo(6f * sx, 12f * sy)
                    close()
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 1.4f * density, join = StrokeJoin.Round),
                )
            }
            MessageOrigin.Module -> {
                val r = 1.2f
                val side = 5.5f
                val stroke = Stroke(width = 1.4f * density)
                listOf(
                    3f to 3f, 9.5f to 3f, 3f to 9.5f, 9.5f to 9.5f,
                ).forEach { (x, y) ->
                    drawRoundRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(x * sx, y * sy),
                        size = androidx.compose.ui.geometry.Size(side * sx, side * sy),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * sx, r * sy),
                        style = stroke,
                    )
                }
            }
            MessageOrigin.Secretariat -> {
                val stroke = Stroke(width = 1.3f * density, cap = StrokeCap.Round)
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(3f * sx, 4f * sy),
                    size = androidx.compose.ui.geometry.Size(12f * sx, 10f * sy),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.4f * sx, 1.4f * sy),
                    style = stroke,
                )
                // flap M3 4l6 4.5L15 4
                val flap = Path().apply {
                    moveTo(3f * sx, 4f * sy)
                    lineTo(9f * sx, 8.5f * sy)
                    lineTo(15f * sx, 4f * sy)
                }
                drawPath(flap, color = color, style = stroke)
                val mark = Path().apply {
                    moveTo(6f * sx, 13f * sy)
                    lineTo(10f * sx, 13f * sy)
                }
                drawPath(mark, color = color, style = stroke)
            }
            MessageOrigin.Campus -> {
                val stroke = Stroke(width = 1.3f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
                // roof diamond
                val roof = Path().apply {
                    moveTo(9f * sx, 2f * sy)
                    lineTo(2f * sx, 6f * sy)
                    lineTo(9f * sx, 9f * sy)
                    lineTo(16f * sx, 6f * sy)
                    close()
                }
                drawPath(roof, color = color, style = stroke)
                // cup under the cap
                val cup = Path().apply {
                    moveTo(4f * sx, 8f * sy)
                    lineTo(4f * sx, 11.5f * sy)
                    quadraticTo(9f * sx, 15.5f * sy, 14f * sx, 11.5f * sy)
                    lineTo(14f * sx, 8f * sy)
                }
                drawPath(cup, color = color, style = stroke)
            }
            else -> Unit
        }
    }
}
