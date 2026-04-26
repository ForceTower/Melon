package dev.forcetower.unes.ui.feature.disciplinedetail.components

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.Attachment
import dev.forcetower.unes.ui.feature.disciplines.AttachmentKind
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.attachmentsForGroup
import dev.forcetower.unes.ui.feature.disciplines.hasMultipleGroups

// Attachments list. Icon per file type, truncation on long names, and a turma
// badge appears for multi-group disciplines when "Tudo" is active. Mirrors
// iOS `DisciplineAttachmentsBlock`.
@Composable
internal fun DisciplineAttachmentsBlock(
    discipline: Discipline,
    selectedGroup: String?,
    modifier: Modifier = Modifier,
) {
    if (discipline.attachments.isEmpty()) return
    val visible = discipline.attachmentsForGroup(selectedGroup)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 18.dp),
    ) {
        DisciplineSectionHeader(title = stringResource(R.string.discipline_detail_attachments_title)) {
            Text(
                text = visible.size.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }

        if (visible.isEmpty()) {
            EmptyCard(text = stringResource(R.string.discipline_detail_attachments_empty))
        } else {
            AttachmentsCard(
                attachments = visible,
                accent = discipline.color,
                showGroupBadge = discipline.hasMultipleGroups && selectedGroup == null,
            )
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .border(1.dp, cardLine, shape)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AttachmentsCard(
    attachments: List<Attachment>,
    accent: Color,
    showGroupBadge: Boolean,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val line = MaterialTheme.melon.surface.line
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .border(1.dp, cardLine, shape),
    ) {
        attachments.forEachIndexed { idx, att ->
            AttachmentRow(
                attachment = att,
                accent = accent,
                showGroupBadge = showGroupBadge && att.group != null,
            )
            if (idx < attachments.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(line),
                )
            }
        }
    }
}

@Composable
private fun AttachmentRow(
    attachment: Attachment,
    accent: Color,
    showGroupBadge: Boolean,
) {
    val context = LocalContext.current
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val openableUrl = attachment.url?.takeIf { it.isNotBlank() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { base ->
                if (openableUrl != null) {
                    base.clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, openableUrl.toUri()),
                            )
                        }
                    }
                } else {
                    base
                }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Tinted icon tile.
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            AttachmentGlyph(kind = attachment.kind, tint = accent)
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = attachment.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val metaText = if (attachment.added.isNotEmpty()) {
                    stringResource(
                        R.string.discipline_detail_attachment_meta_with_date_format,
                        attachment.kind.label,
                        attachment.added,
                    )
                } else {
                    stringResource(
                        R.string.discipline_detail_attachment_meta_kind_only_format,
                        attachment.kind.label,
                    )
                }
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    ),
                    color = ink4,
                )
                if (showGroupBadge && attachment.group != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = attachment.group,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.54.sp,
                            ),
                            color = ink3,
                        )
                    }
                }
            }
        }

        // Trailing chevron.
        Canvas(modifier = Modifier.size(14.dp)) {
            val strokePx = 1.5.dp.toPx()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val arm = size.minDimension * 0.32f
            val path = Path().apply {
                moveTo(cx - arm * 0.4f, cy - arm)
                lineTo(cx + arm * 0.4f, cy)
                lineTo(cx - arm * 0.4f, cy + arm)
            }
            drawPath(
                path = path,
                color = ink3,
                style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
private fun AttachmentGlyph(kind: AttachmentKind, tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val strokePx = 1.3.dp.toPx()
        val w = size.width
        val h = size.height
        when (kind) {
            AttachmentKind.Pdf -> {
                // Folded document outline.
                val path = Path().apply {
                    moveTo(w * 0.22f, h * 0.10f)
                    lineTo(w * 0.55f, h * 0.10f)
                    lineTo(w * 0.78f, h * 0.34f)
                    lineTo(w * 0.78f, h * 0.92f)
                    lineTo(w * 0.22f, h * 0.92f)
                    close()
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = strokePx, join = StrokeJoin.Round),
                )
                // Fold corner.
                drawLine(
                    color = tint,
                    start = Offset(w * 0.55f, h * 0.10f),
                    end = Offset(w * 0.55f, h * 0.34f),
                    strokeWidth = strokePx,
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.55f, h * 0.34f),
                    end = Offset(w * 0.78f, h * 0.34f),
                    strokeWidth = strokePx,
                )
            }
            AttachmentKind.Slides -> {
                // Slide rectangle with horizontal text rules.
                drawRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.56f),
                    style = Stroke(width = strokePx),
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.30f, h * 0.45f),
                    end = Offset(w * 0.70f, h * 0.45f),
                    strokeWidth = strokePx,
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.30f, h * 0.62f),
                    end = Offset(w * 0.58f, h * 0.62f),
                    strokeWidth = strokePx,
                )
            }
            AttachmentKind.Link -> {
                // Two interlocked link rings — approximated with strokes.
                val arc = Path().apply {
                    moveTo(w * 0.55f, h * 0.28f)
                    lineTo(w * 0.72f, h * 0.28f)
                    cubicTo(
                        w * 0.92f, h * 0.28f,
                        w * 0.92f, h * 0.72f,
                        w * 0.72f, h * 0.72f,
                    )
                    lineTo(w * 0.55f, h * 0.72f)
                }
                drawPath(arc, tint, style = Stroke(width = strokePx, cap = StrokeCap.Round))
                val arc2 = Path().apply {
                    moveTo(w * 0.45f, h * 0.72f)
                    lineTo(w * 0.28f, h * 0.72f)
                    cubicTo(
                        w * 0.08f, h * 0.72f,
                        w * 0.08f, h * 0.28f,
                        w * 0.28f, h * 0.28f,
                    )
                    lineTo(w * 0.45f, h * 0.28f)
                }
                drawPath(arc2, tint, style = Stroke(width = strokePx, cap = StrokeCap.Round))
                drawLine(
                    color = tint,
                    start = Offset(w * 0.38f, h * 0.50f),
                    end = Offset(w * 0.62f, h * 0.50f),
                    strokeWidth = strokePx,
                )
            }
            AttachmentKind.Notes, AttachmentKind.Other -> {
                // Lined notepad rectangle.
                drawRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.18f),
                    size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.64f),
                    style = Stroke(width = strokePx),
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.30f, h * 0.40f),
                    end = Offset(w * 0.70f, h * 0.40f),
                    strokeWidth = strokePx,
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.30f, h * 0.55f),
                    end = Offset(w * 0.65f, h * 0.55f),
                    strokeWidth = strokePx,
                )
            }
        }
    }
}
