package dev.forcetower.unes.ui.feature.disciplinedetail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.Attachment
import dev.forcetower.unes.ui.feature.disciplines.AttachmentKind
import dev.forcetower.unes.ui.feature.disciplines.ClassEntry
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.attachmentsForGroup
import dev.forcetower.unes.ui.feature.disciplines.classesForGroup

// M3 secondary tabs closing the detail screen — "Materiais" (files published
// by the teacher on SAGRES) and "Aulas" (the lesson plan timeline).
@Composable
internal fun DisciplineDetailTabs(
    discipline: Discipline,
    selectedGroup: String?,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            listOf(
                stringResource(R.string.discipline_detail_tab_materials),
                stringResource(R.string.discipline_detail_tab_classes),
            ).forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == index) FontWeight.ExtraBold else FontWeight.SemiBold,
                            ),
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            when (selectedTab) {
                0 -> MateriaisTab(
                    attachments = discipline.attachmentsForGroup(selectedGroup),
                    subject = discipline.color,
                )
                else -> AulasTab(
                    classes = discipline.classesForGroup(selectedGroup),
                    subject = discipline.color,
                )
            }
        }
    }
}

// ── Materiais (teacher-published lecture files) ──────────────────────────────

@Composable
private fun MateriaisTab(
    attachments: List<Attachment>,
    subject: Color,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.discipline_detail_attachments_header),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (attachments.isNotEmpty()) {
                Text(
                    text = pluralStringResource(
                        R.plurals.discipline_detail_attachments_count,
                        attachments.size,
                        attachments.size,
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val shape = RoundedCornerShape(22.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.melon.surface.card)
                .border(1.dp, MaterialTheme.melon.surface.cardLine, shape),
        ) {
            if (attachments.isEmpty()) {
                Text(
                    text = stringResource(R.string.discipline_detail_attachments_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                )
            }
            attachments.forEachIndexed { index, attachment ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.melon.surface.line)
                AttachmentRow(attachment = attachment, subject = subject)
            }
        }
    }
}

@Composable
private fun AttachmentRow(attachment: Attachment, subject: Color) {
    val uriHandler = LocalUriHandler.current
    val url = attachment.url
    val openLabel = stringResource(R.string.discipline_detail_attachment_open)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = url != null) {
                if (url != null) runCatching { uriHandler.openUri(url) }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(subject.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = attachment.kind.icon,
                contentDescription = null,
                tint = subject,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = if (attachment.added.isNotEmpty()) {
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
                text = meta,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Icon(
            imageVector = Icons.Filled.Download,
            contentDescription = openLabel,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

private val AttachmentKind.icon: ImageVector
    get() = when (this) {
        AttachmentKind.Pdf -> Icons.Filled.PictureAsPdf
        AttachmentKind.Slides -> Icons.Filled.Slideshow
        AttachmentKind.Notes -> Icons.AutoMirrored.Filled.StickyNote2
        AttachmentKind.Link -> Icons.Filled.Link
        AttachmentKind.Other -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

// ── Aulas (lesson plan timeline) ─────────────────────────────────────────────

// Collapsed by default to a window around today (1 past + current + 3
// upcoming); "Ver todas as N aulas" expands the full plan.
private const val CollapsedWindow = 5

@Composable
private fun AulasTab(
    classes: List<ClassEntry>,
    subject: Color,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val visible = if (expanded) classes else windowAroundCurrent(classes)

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.discipline_detail_classes_header),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (classes.isNotEmpty()) {
                Text(
                    text = pluralStringResource(
                        R.plurals.discipline_detail_classes_count,
                        classes.size,
                        classes.size,
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (classes.isEmpty()) {
            val shape = RoundedCornerShape(22.dp)
            Text(
                text = stringResource(R.string.discipline_detail_classes_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.melon.surface.card)
                    .border(1.dp, MaterialTheme.melon.surface.cardLine, shape)
                    .padding(20.dp),
            )
            return
        }

        Column {
            visible.forEachIndexed { index, entry ->
                AulaRow(
                    entry = entry,
                    subject = subject,
                    isFirst = index == 0,
                    isLast = index == visible.size - 1,
                )
            }
        }

        if (classes.size > CollapsedWindow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Start-aligned with the lecture cards (dot column + gap),
                    // clear of the last card's bottom edge.
                    .padding(start = 26.dp, top = 16.dp)
                    .height(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.melon.surface.line, CircleShape)
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expanded) {
                        stringResource(R.string.discipline_detail_classes_show_less)
                    } else {
                        pluralStringResource(R.plurals.discipline_detail_classes_show_all_format, classes.size, classes.size)
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// 1 past + the current lecture + 3 upcoming. All-past plans (semester over)
// fall back to the most recent entries.
private fun windowAroundCurrent(classes: List<ClassEntry>): List<ClassEntry> {
    if (classes.size <= CollapsedWindow) return classes
    val anchor = classes.indexOfFirst { it.isNext }
    if (anchor < 0) return classes.takeLast(CollapsedWindow)
    val start = (anchor - 1).coerceAtLeast(0)
    val end = (start + CollapsedWindow).coerceAtMost(classes.size)
    return classes.subList(start, end)
}

@Composable
private fun AulaRow(
    entry: ClassEntry,
    subject: Color,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val accent = MaterialTheme.colorScheme.primary
    val card = MaterialTheme.melon.surface.card
    val line = MaterialTheme.melon.surface.line
    val shape = RoundedCornerShape(16.dp)
    val future = !entry.past && !entry.isNext

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TimelineDot(
            subject = subject,
            accent = accent,
            line = line,
            past = entry.past,
            isNext = entry.isNext,
            isFirst = isFirst,
            isLast = isLast,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 14.dp)
                .alpha(if (future) 0.75f else 1f)
                .clip(shape)
                .background(if (entry.isNext) accent.copy(alpha = 0.08f).compositeOver(card) else card)
                .border(
                    width = if (entry.isNext) 1.5.dp else 1.dp,
                    color = if (entry.isNext) accent.copy(alpha = 0.40f) else MaterialTheme.melon.surface.cardLine,
                    shape = shape,
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (entry.ordinal > 0) {
                    Text(
                        text = stringResource(R.string.discipline_detail_class_number_format, entry.ordinal),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp,
                        ),
                        color = if (future) MaterialTheme.colorScheme.outlineVariant else subject,
                    )
                }
                Text(
                    text = entry.date ?: stringResource(R.string.discipline_detail_classes_no_date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                if (entry.isNext) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = stringResource(R.string.discipline_detail_class_next_pill).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp,
                            ),
                            color = accent,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Text(
                text = entry.title.ifEmpty { stringResource(R.string.discipline_detail_classes_no_title) },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (entry.isNext) FontWeight.Bold else FontWeight.SemiBold,
                    lineHeight = 19.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

// Timeline spine: done lectures get a filled subject dot, the next one an
// accent dot with a halo, future ones a hollow outline.
@Composable
private fun TimelineDot(
    subject: Color,
    accent: Color,
    line: Color,
    past: Boolean,
    isNext: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val background = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = Modifier
            .width(14.dp)
            .fillMaxHeight(),
    ) {
        val centerX = size.width / 2f
        val dotCenterY = 22.dp.toPx()
        val radius = (if (isNext) 6.dp else 5.dp).toPx()

        if (!isFirst) {
            drawLine(
                color = line,
                start = Offset(centerX, 0f),
                end = Offset(centerX, dotCenterY - radius),
                strokeWidth = 2.dp.toPx(),
            )
        }
        if (!isLast) {
            drawLine(
                color = line,
                start = Offset(centerX, dotCenterY + radius),
                end = Offset(centerX, size.height),
                strokeWidth = 2.dp.toPx(),
            )
        }

        when {
            isNext -> {
                drawCircle(
                    color = accent.copy(alpha = 0.22f),
                    radius = radius + 3.dp.toPx(),
                    center = Offset(centerX, dotCenterY),
                )
                drawCircle(color = accent, radius = radius, center = Offset(centerX, dotCenterY))
            }
            past -> drawCircle(color = subject, radius = radius, center = Offset(centerX, dotCenterY))
            else -> {
                drawCircle(color = background, radius = radius, center = Offset(centerX, dotCenterY))
                drawCircle(
                    color = outline,
                    radius = radius,
                    center = Offset(centerX, dotCenterY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
            }
        }
    }
}
