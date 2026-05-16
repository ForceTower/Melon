package dev.forcetower.unes.ui.feature.messages.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.ui.feature.messages.Message
import dev.forcetower.unes.ui.feature.messages.MessageAttachment
import dev.forcetower.unes.ui.feature.messages.MessageAttachmentKind
import dev.forcetower.unes.ui.feature.messages.RelativeTime
import dev.forcetower.unes.ui.feature.messages.originMeta
import dev.forcetower.unes.ui.feature.messages.relativeTime

// One row inside a bucket card. Shows the origin swatch, a sender/time
// header, the origin-colored meta line, an optional subject, the preview,
// and a footer with attachment hints and a star indicator.
@Composable
internal fun MessageRow(
    message: Message,
    onOpen: (Message) -> Unit,
    modifier: Modifier = Modifier,
) {
    val meta = originMeta(message)
    val ink = MaterialTheme.colorScheme.onBackground
    val ink2 = MaterialTheme.colorScheme.onSurface
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val coral = MaterialTheme.colorScheme.primary

    val preview = message.preview?.takeIf { it.isNotBlank() }
        ?: message.body.replace(Regex("\\n+"), " ").trim()

    Box(modifier = modifier.fillMaxWidth().clickable { onOpen(message) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            OriginSwatch(message = message)

            Column(modifier = Modifier.weight(1f)) {
                // Top line: sender + relative time
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = message.sender.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            fontWeight = if (message.unread) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = (-0.13).sp,
                        ),
                        color = ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    RelTimeLabel(time = relativeTime(message.receivedAt), color = ink4)
                }

                // Origin context line (mono, uppercase, accent-colored)
                Text(
                    text = message.sender.role,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.9.sp,
                    ),
                    color = meta.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                if (!message.subject.isNullOrBlank()) {
                    Text(
                        text = message.subject,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = if (message.unread) FontWeight.SemiBold else FontWeight.Medium,
                            lineHeight = 18.sp,
                        ),
                        color = ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }

                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = if (message.subject != null) 12.sp else 13.sp,
                        fontWeight = if (message.subject == null && message.unread) FontWeight.Medium else FontWeight.Normal,
                        lineHeight = if (message.subject != null) 17.sp else 19.sp,
                    ),
                    color = if (message.subject != null) ink3 else ink2,
                    maxLines = if (message.subject != null) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                )

                if (message.attachments.isNotEmpty() || message.starred) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AttachHint(attachments = message.attachments, color = ink4)
                        if (message.starred) {
                            StarGlyph(color = StarColor)
                        }
                    }
                }
            }
        }

        // Unread dot — sits absolute to the row, like the JSX prototype.
        if (message.unread) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, top = 22.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(coral),
            )
        }
    }
}

@Composable
private fun RelTimeLabel(time: RelativeTime, color: Color) {
    val text = when (time) {
        is RelativeTime.Literal -> time.text
        is RelativeTime.Resource -> if (time.arg != null) stringResource(time.res, time.arg) else stringResource(time.res)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 0.4.sp,
        ),
        color = color,
        maxLines = 1,
    )
}

// Attachment hint shown in the row footer — mono count for files and a small
// image icon with count for images.
@Composable
internal fun AttachHint(
    attachments: List<MessageAttachment>,
    color: Color,
) {
    if (attachments.isEmpty()) return
    val images = attachments.count { it.kind == MessageAttachmentKind.Image }
    val files = attachments.size - images

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (files > 0) {
            PaperclipGlyph(color = color)
            Text(
                text = files.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                ),
                color = color,
            )
        }
        if (files > 0 && images > 0) {
            Text(
                text = stringResource(R.string.messages_attach_separator),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = color.copy(alpha = 0.4f),
            )
        }
        if (images > 0) {
            ImageGlyph(color = color)
            Text(
                text = images.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                ),
                color = color,
            )
        }
    }
}

@Composable
private fun PaperclipGlyph(color: Color) {
    Canvas(modifier = Modifier.size(10.dp)) {
        val w = size.width
        val h = size.height
        val sx = w / 10f
        val sy = h / 10f
        val stroke = Stroke(width = 1f * density, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(7f * sx, 2.5f * sy)
            lineTo(3.5f * sx, 6f * sy)
            quadraticTo(2.5f * sx, 7f * sy, 3.5f * sx, 8f * sy)
            quadraticTo(4.5f * sx, 9f * sy, 5.5f * sx, 8f * sy)
            lineTo(9f * sx, 4.5f * sy)
            quadraticTo(10f * sx, 3.5f * sy, 8.5f * sx, 2f * sy)
            quadraticTo(7f * sx, 0.5f * sy, 5.5f * sx, 2f * sy)
            lineTo(2f * sx, 4.5f * sy)
            quadraticTo(0.5f * sx, 6f * sy, 2f * sx, 8f * sy)
        }
        drawPath(path, color = color, style = stroke)
    }
}

@Composable
private fun ImageGlyph(color: Color) {
    Canvas(modifier = Modifier.size(10.dp)) {
        val w = size.width
        val h = size.height
        val sx = w / 10f
        val sy = h / 10f
        val stroke = Stroke(width = 1f * density, join = StrokeJoin.Round)
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(1f * sx, 1.5f * sy),
            size = androidx.compose.ui.geometry.Size(8f * sx, 7f * sy),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f * sx, 1f * sy),
            style = stroke,
        )
        drawCircle(
            color = color,
            radius = 0.6f * sx,
            center = androidx.compose.ui.geometry.Offset(3.5f * sx, 4f * sy),
        )
        val mountains = Path().apply {
            moveTo(1f * sx, 7f * sy)
            lineTo(3.5f * sx, 5f * sy)
            lineTo(5.5f * sx, 6.5f * sy)
            lineTo(7f * sx, 5f * sy)
            lineTo(9f * sx, 7f * sy)
        }
        drawPath(mountains, color = color, style = stroke)
    }
}

@Composable
private fun StarGlyph(color: Color) {
    Canvas(modifier = Modifier.size(11.dp)) {
        val w = size.width
        val h = size.height
        val sx = w / 11f
        val sy = h / 11f
        val path = Path().apply {
            moveTo(5.5f * sx, 1f * sy)
            lineTo(6.8f * sx, 3.7f * sy)
            lineTo(9.75f * sx, 4f * sy)
            lineTo(7.55f * sx, 6f * sy)
            lineTo(8.15f * sx, 8.85f * sy)
            lineTo(5.5f * sx, 7.6f * sy)
            lineTo(2.85f * sx, 9f * sy)
            lineTo(3.45f * sx, 6.15f * sy)
            lineTo(1.25f * sx, 4.15f * sy)
            lineTo(4.2f * sx, 3.85f * sy)
            close()
        }
        drawPath(path, color = color)
    }
}

// Star tone on iOS / JSX is a fixed amber (#D9852E). It's an indicator hue,
// not a palette slot, so keep it as a literal here rather than aliasing it
// onto a brand color that has different semantics elsewhere.
private val StarColor = Color(0xFFD9852E)
