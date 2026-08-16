package dev.forcetower.unes.ui.feature.courseprogress.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumEntry
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumEntryStatus
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.courseprogress.CurriculumStatusBadge
import dev.forcetower.unes.ui.feature.courseprogress.curriculumStatusStyle
import dev.forcetower.unes.ui.feature.courseprogress.trailDim

// Shared surfaces for "Progresso do curso" and the fluxograma: the card
// plate every section sits on, the section headers, the tonal notice banners,
// the situation chips/legend, and the discipline row used by the períodos
// list, the trail groups and the detail sheet.

@Composable
internal fun CourseProgressCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape),
        content = content,
    )
}

@Composable
internal fun CourseProgressSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Column(modifier = modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.38).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

// Tonal advisory banner — the stale-grid warning, the missing-curriculum note,
// the missing-breakdown note, and the per-situation notices in the sheet.
@Composable
internal fun CourseProgressNotice(
    tone: Color,
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tone.copy(alpha = 0.10f))
            .border(1.dp, tone.copy(alpha = 0.28f), shape)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
internal fun CurriculumStatusChip(
    status: CurriculumEntryStatus,
    modifier: Modifier = Modifier,
    full: Boolean = false,
) {
    val style = curriculumStatusStyle(status)
    val neutral = status == CurriculumEntryStatus.NotTaken
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                if (neutral) {
                    MaterialTheme.colorScheme.surfaceContainer
                } else {
                    style.tone.copy(alpha = 0.12f)
                },
            )
            .border(
                1.dp,
                if (neutral) MaterialTheme.melon.surface.line else style.tone.copy(alpha = 0.32f),
                shape,
            )
            .padding(start = 7.dp, end = 9.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = if (neutral) MaterialTheme.colorScheme.outline else style.tone,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = if (full) style.label else style.shortLabel,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
            color = if (neutral) MaterialTheme.colorScheme.outline else style.tone,
            maxLines = 1,
        )
    }
}

// Every situation, spelled out. Order is the enum's own declaration order:
// what is done, what is happening, what can be picked, then what is stuck.
@Composable
internal fun CurriculumLegend(modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        CurriculumEntryStatus.entries.forEach { status ->
            CurriculumStatusChip(status = status, full = true)
        }
    }
}

// One discipline as a list row — badge, name, and a metadata line. Used by
// the períodos lens, the trail groups, and the "depende de" / "libera" /
// "cursar junto" lists in the sheet.
@Composable
internal fun CurriculumEntryRow(
    entry: CurriculumEntry,
    meta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    emphasized: Boolean = false,
    dimmed: Boolean = false,
) {
    Column(modifier = modifier.trailDim(dimmed)) {
        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.melon.surface.line,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CurriculumStatusBadge(status = entry.status, size = 26.dp, corner = 8.dp, iconSize = 15.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 17.sp,
                        fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold,
                        letterSpacing = (-0.16).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// A fixed-height track that reads as "no scale here": the hatched fill the
// design uses for buckets the portal can't measure and for a curriculum with
// no known total.
@Composable
internal fun HatchedTrack(modifier: Modifier = Modifier, height: Dp = 8.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .hatchStripes(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    )
}

private fun Modifier.hatchStripes(color: Color): Modifier = drawBehind {
    val step = 9.dp.toPx()
    var x = -size.height
    while (x < size.width) {
        drawLine(
            color = color,
            start = Offset(x, size.height),
            end = Offset(x + size.height, 0f),
            strokeWidth = 4.dp.toPx(),
        )
        x += step
    }
}
