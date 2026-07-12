package dev.forcetower.unes.ui.feature.schedule.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.schedule.ScheduleClass
import dev.forcetower.unes.ui.feature.schedule.durationMin
import dev.forcetower.unes.ui.feature.schedule.endMin
import dev.forcetower.unes.ui.feature.schedule.startMin

// Connected timeline for the selected day (dc `UNES Horário - Android`): a
// vertical spine linking one tonal card per class, with "livre" gap dividers
// between non-adjacent sessions. Tapping a card expands the quick-actions
// row (Turma · Materiais · Lembrar).
@Composable
internal fun ScheduleTimeline(
    classes: List<ScheduleClass>,
    expandedId: String?,
    onToggle: (String) -> Unit,
    onOpenDiscipline: (ScheduleClass) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
    ) {
        // Spine behind the nodes — x centers on the 16dp node column that
        // follows the 52dp time column.
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(start = 59.dp, top = 16.dp, bottom = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(MaterialTheme.melon.surface.line),
            )
        }

        Column {
            classes.forEachIndexed { index, cls ->
                val prev = classes.getOrNull(index - 1)
                val gapMin = prev?.let { cls.startMin - it.endMin } ?: 0
                val rowDelayMs = index * 50
                if (gapMin > 0) {
                    GapDivider(
                        gapMin = gapMin,
                        modifier = Modifier.fadeUpOnAppear(
                            delayMs = rowDelayMs,
                            durationMs = 400,
                            fromOffset = 10.dp,
                        ),
                    )
                }
                val id = "${cls.code}-$index"
                ClassRow(
                    cls = cls,
                    expanded = expandedId == id,
                    onToggle = { onToggle(id) },
                    onOpenDiscipline = onOpenDiscipline,
                    modifier = Modifier.fadeUpOnAppear(
                        delayMs = rowDelayMs,
                        durationMs = 400,
                        fromOffset = 10.dp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun GapDivider(gapMin: Int, modifier: Modifier = Modifier) {
    val ink3 = MaterialTheme.colorScheme.outline
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val line = MaterialTheme.melon.surface.line
    val label = remember(gapMin) { formatHourLabel(gapMin) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(52.dp))
        Box(
            modifier = Modifier.width(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(ink4),
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Coffee,
                contentDescription = null,
                tint = ink4,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = stringResource(R.string.schedule_gap_free_format, label),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = ink3,
                maxLines = 1,
            )
            DashedLine(
                color = line,
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp),
            )
        }
    }
}

@Composable
private fun DashedLine(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val dash = 6.dp.toPx()
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
        )
    }
}

@Composable
private fun ClassRow(
    cls: ScheduleClass,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenDiscipline: (ScheduleClass) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .width(52.dp)
                .padding(end = 12.dp, top = 14.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = cls.start,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = ink,
            )
            Text(
                text = cls.end,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = ink4,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Box(
            modifier = Modifier
                .width(16.dp)
                .padding(top = 18.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(cls.color),
            )
        }
        ClassCard(
            cls = cls,
            expanded = expanded,
            onToggle = onToggle,
            onOpenDiscipline = onOpenDiscipline,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
        )
    }
}

@Composable
private fun ClassCard(
    cls: ScheduleClass,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenDiscipline: (ScheduleClass) -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val ink = MaterialTheme.colorScheme.onBackground
    val line = MaterialTheme.melon.surface.line
    val shape = RoundedCornerShape(20.dp)
    val borderColor by animateColorAsState(
        targetValue = cls.color.copy(alpha = if (expanded) 0.42f else 0.26f),
        animationSpec = tween(200),
        label = "card-border",
    )

    Column(
        modifier = modifier
            .clip(shape)
            .background(cls.color.copy(alpha = 0.09f).compositeOver(card))
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CodeChip(code = cls.code, color = cls.color)
            Spacer(Modifier.weight(1f))
            Text(
                text = formatHourLabel(cls.durationMin),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
                maxLines = 1,
            )
        }

        Text(
            text = cls.title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                lineHeight = 20.sp,
                letterSpacing = (-0.17).sp,
                fontWeight = FontWeight.Bold,
            ),
            color = ink,
        )

        cls.topic?.let { topic ->
            NoteChip(
                note = topic,
                color = cls.color,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        FooterRow(cls = cls, modifier = Modifier.padding(top = 11.dp))

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(220)) + expandVertically(tween(220)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(180)),
        ) {
            Column(modifier = Modifier.padding(top = 13.dp)) {
                HorizontalDivider(thickness = 1.dp, color = line)
                Row(
                    modifier = Modifier.padding(top = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickAction(
                        icon = Icons.Outlined.Groups,
                        label = stringResource(R.string.schedule_action_class),
                        color = cls.color,
                        onClick = if (cls.offerId != null) {
                            { onOpenDiscipline(cls) }
                        } else null,
                        modifier = Modifier.weight(1f),
                    )
                    // Materiais renders per the dc spec but stays inert until
                    // the Android Materiais feature lands.
                    QuickAction(
                        icon = Icons.Outlined.FolderOpen,
                        label = stringResource(R.string.schedule_action_materials),
                        color = cls.color,
                        onClick = null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeChip(code: String, color: Color) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.20f))
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.48.sp,
            ),
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun NoteChip(note: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(start = 9.dp, end = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.EditNote,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = note,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FooterRow(cls: ScheduleClass, modifier: Modifier = Modifier) {
    val ink2 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink3 = MaterialTheme.colorScheme.outline
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val location = remember(cls) {
        listOfNotNull(cls.modulo, cls.room, cls.campus).joinToString(" · ")
    }
    val initials = remember(cls.prof) { profInitials(cls.prof) }

    // Location and teacher stack on their own lines (unlike the dc mock's
    // single footer row) — real data runs much longer than "T. Pires", so
    // both lines wrap instead of fighting for one row.
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = ink4,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(16.dp),
            )
            Text(
                text = location.ifEmpty { stringResource(R.string.schedule_location_unknown) },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                ),
                color = ink3,
            )
        }
        if (initials.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(cls.color.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = cls.color,
                    )
                }
                Text(
                    text = cls.prof,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = ink2,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

// "2h", "1h40", "45min" — hour-first compact duration/gap label.
internal fun formatHourLabel(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> "${hours}h${rest.toString().padStart(2, '0')}"
        hours > 0 -> "${hours}h"
        else -> "${rest}min"
    }
}

// "T. Pires" → "TP". First letter of the first two name tokens.
private fun profInitials(prof: String): String =
    prof.split(" ")
        .mapNotNull { token -> token.firstOrNull { it.isLetter() } }
        .take(2)
        .joinToString("")
        .uppercase()
