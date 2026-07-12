package dev.forcetower.unes.ui.feature.messages.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.messages.MessageAttachment
import dev.forcetower.unes.ui.feature.messages.MessageAttachmentKind

// Attachment tile used on the message detail screen. Three layouts:
//
// * `Image` — a 16:10 gradient panel with a picture glyph in the accent.
// * `Link`  — title + host pair with a link-chain icon.
// * default — file name + size, with a download arrow.
@Composable
internal fun AttachmentTile(
    attachment: MessageAttachment,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    when (attachment.kind) {
        MessageAttachmentKind.Image -> ImageTile(accent = accent, modifier = modifier)
        MessageAttachmentKind.Link -> LinkTile(attachment = attachment, accent = accent, modifier = modifier)
        else -> FileTile(attachment = attachment, accent = accent, modifier = modifier)
    }
}

@Composable
private fun ImageTile(accent: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(accent.copy(alpha = 0.20f), accent.copy(alpha = 0.07f)),
                ),
            )
            .border(1.dp, MaterialTheme.melon.surface.cardLine, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        ImageGlyph(color = accent, size = 28.dp)
    }
}

@Composable
private fun LinkTile(attachment: MessageAttachment, accent: Color, modifier: Modifier) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Badge(accent = accent) { LinkGlyph(color = accent) }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = attachment.title.orEmpty(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = attachment.host.orEmpty(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.3.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FileTile(attachment: MessageAttachment, accent: Color, modifier: Modifier) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Badge(accent = accent) {
            if (attachment.kind == MessageAttachmentKind.Slides) {
                SlidesGlyph(color = accent)
            } else {
                DocGlyph(color = accent)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = attachment.name.orEmpty(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val parts = buildList {
                add(attachment.kind.label)
                attachment.size?.let { add(it) }
            }
            Text(
                text = parts.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.3.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        DownloadArrowGlyph(color = ink3)
    }
}

@Composable
private fun Badge(accent: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ImageGlyph(color: Color, size: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sx = w / 28f
        val sy = h / 28f
        val stroke = Stroke(width = 1.4f * density, join = StrokeJoin.Round)
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(3f * sx, 5f * sy),
            size = androidx.compose.ui.geometry.Size(22f * sx, 18f * sy),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * sx, 2f * sy),
            style = stroke,
        )
        drawCircle(
            color = color,
            radius = 1.5f * sx,
            center = androidx.compose.ui.geometry.Offset(9f * sx, 11f * sy),
        )
        val mountains = Path().apply {
            moveTo(3f * sx, 19f * sy)
            lineTo(10f * sx, 13f * sy)
            lineTo(15f * sx, 17f * sy)
            lineTo(19f * sx, 14f * sy)
            lineTo(25f * sx, 19f * sy)
        }
        drawPath(mountains, color = color, style = stroke)
    }
}

@Composable
private fun LinkGlyph(color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val sx = w / 16f
        val sy = h / 16f
        val stroke = Stroke(width = 1.4f * density, cap = StrokeCap.Round)
        // Two interlocking arcs
        val l1 = Path().apply {
            moveTo(7f * sx, 9f * sy)
            lineTo(5f * sx, 11f * sy)
            quadraticTo(3f * sx, 13f * sy, 5f * sx, 11f * sy)
            quadraticTo(2f * sx, 8f * sy, 4f * sx, 6f * sy)
            lineTo(6f * sx, 4f * sy)
        }
        drawPath(l1, color = color, style = stroke)
        val l2 = Path().apply {
            moveTo(9f * sx, 7f * sy)
            lineTo(11f * sx, 5f * sy)
            quadraticTo(13f * sx, 3f * sy, 11f * sx, 5f * sy)
            quadraticTo(14f * sx, 8f * sy, 12f * sx, 10f * sy)
            lineTo(10f * sx, 12f * sy)
        }
        drawPath(l2, color = color, style = stroke)
        val mid = Path().apply {
            moveTo(6f * sx, 10f * sy)
            lineTo(10f * sx, 6f * sy)
        }
        drawPath(mid, color = color, style = stroke)
    }
}

@Composable
private fun DocGlyph(color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val sx = w / 16f
        val sy = h / 16f
        val stroke = Stroke(width = 1.4f * density, join = StrokeJoin.Round)
        val outline = Path().apply {
            moveTo(4f * sx, 2f * sy)
            lineTo(10f * sx, 2f * sy)
            lineTo(13f * sx, 5f * sy)
            lineTo(13f * sx, 14f * sy)
            lineTo(4f * sx, 14f * sy)
            close()
        }
        drawPath(outline, color = color, style = stroke)
        val fold = Path().apply {
            moveTo(10f * sx, 2f * sy)
            lineTo(10f * sx, 5f * sy)
            lineTo(13f * sx, 5f * sy)
        }
        drawPath(fold, color = color, style = stroke)
    }
}

@Composable
private fun SlidesGlyph(color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val sx = w / 16f
        val sy = h / 16f
        val stroke = Stroke(width = 1.4f * density, cap = StrokeCap.Round)
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(2f * sx, 3f * sy),
            size = androidx.compose.ui.geometry.Size(12f * sx, 9f * sy),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.2f * sx, 1.2f * sy),
            style = stroke,
        )
        // stand
        val stand = Path().apply {
            moveTo(2f * sx, 12f * sy)
            lineTo(2f * sx, 14f * sy)
            lineTo(14f * sx, 14f * sy)
            lineTo(14f * sx, 12f * sy)
        }
        drawPath(stand, color = color, style = stroke)
        // legs
        val legs = Path().apply {
            moveTo(5f * sx, 14f * sy)
            lineTo(5f * sx, 15f * sy)
            moveTo(11f * sx, 14f * sy)
            lineTo(11f * sx, 15f * sy)
        }
        drawPath(legs, color = color, style = stroke)
    }
}

@Composable
private fun DownloadArrowGlyph(color: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val sx = w / 14f
        val sy = h / 14f
        val stroke = Stroke(width = 1.4f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(7f * sx, 2f * sy)
            lineTo(7f * sx, 11f * sy)
            moveTo(3.5f * sx, 7.5f * sy)
            lineTo(7f * sx, 11f * sy)
            lineTo(10.5f * sx, 7.5f * sy)
        }
        drawPath(path, color = color, style = stroke)
    }
}
