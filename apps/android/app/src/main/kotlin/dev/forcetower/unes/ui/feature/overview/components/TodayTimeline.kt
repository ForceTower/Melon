package dev.forcetower.unes.ui.feature.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.overview.OverviewClassState
import dev.forcetower.unes.ui.feature.overview.OverviewFixtures
import dev.forcetower.unes.ui.feature.overview.OverviewTodayItem

private val TimeGutterWidth = 42.dp
private val TimeGutterSpacing = 7.dp
private val DotColumnWidth = 14.dp

// "Seu dia" — the vertical timeline of today's classes: time gutter, spine
// with state dots, and one card per class.
@Composable
internal fun TodayTimeline(
    items: List<OverviewTodayItem>,
    weekdayLabel: String,
    onOpenClass: (OverviewTodayItem) -> Unit,
    onOpenSchedule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenSchedule),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.overview_today_title).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.overview_today_summary,
                    items.size,
                    weekdayLabel,
                    items.size,
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.height(14.dp))

        Box {
            // Spine behind the dots, inset from the first/last card edges.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(top = 18.dp, bottom = 18.dp),
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = TimeGutterWidth + TimeGutterSpacing + (DotColumnWidth - 2.dp) / 2)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.melon.surface.line),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { item ->
                    TimelineRow(item = item, onClick = { onOpenClass(item) })
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(item: OverviewTodayItem, onClick: () -> Unit) {
    val highlighted = item.state == OverviewClassState.Next || item.state == OverviewClassState.Now
    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier
                .width(TimeGutterWidth)
                .padding(top = 13.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = item.startTime.take(5),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = if (highlighted) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = if (highlighted) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.outline
                },
                textAlign = TextAlign.End,
            )
            item.endTime?.let { end ->
                Text(
                    text = end.take(5),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    textAlign = TextAlign.End,
                )
            }
        }
        Spacer(Modifier.width(TimeGutterSpacing))
        Box(
            modifier = Modifier
                .width(DotColumnWidth)
                .padding(top = if (highlighted) 13.dp else 15.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            TimelineDot(highlighted = highlighted)
        }
        Spacer(Modifier.width(6.dp))
        when (item.state) {
            OverviewClassState.Done -> DoneCard(item, onClick)
            OverviewClassState.Now,
            OverviewClassState.Next,
            -> HighlightCard(item, onClick)
            OverviewClassState.Later -> LaterCard(item, onClick)
        }
    }
}

@Composable
private fun TimelineDot(highlighted: Boolean) {
    if (highlighted) {
        // 11dp accent dot inside a 3dp soft ring — the design's
        // `box-shadow: 0 0 0 3px accent22` glow.
        val accent = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .size(17.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        )
    }
}

@Composable
private fun RowScopeCardBase(
    item: OverviewTodayItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleWeight: FontWeight,
    titleColor: Color,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .padding(vertical = 0.dp)
            .clickable(enabled = item.offerId != null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = titleWeight,
                    fontSize = 15.sp,
                ),
                color = titleColor,
                maxLines = 1,
            )
            item.room?.let { room ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = room,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                )
            }
        }
        trailing()
    }
}

@Composable
private fun RowScope.DoneCard(item: OverviewTodayItem, onClick: () -> Unit) {
    RowScopeCardBase(
        item = item,
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .alpha(0.68f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, RoundedCornerShape(16.dp)),
        titleWeight = FontWeight.Medium,
        titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = stringResource(R.string.overview_today_done_label),
            tint = MaterialTheme.melon.fixed.success,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun RowScope.HighlightCard(item: OverviewTodayItem, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)
    RowScopeCardBase(
        item = item,
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .background(accent.copy(alpha = 0.08f).compositeOver(MaterialTheme.melon.surface.card))
            .border(1.5.dp, accent.copy(alpha = 0.45f), shape),
        titleWeight = FontWeight.Bold,
        titleColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Text(
            text = stringResource(
                if (item.state == OverviewClassState.Now) {
                    R.string.overview_today_now_badge
                } else {
                    R.string.overview_today_next_badge
                },
            ),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
            color = MaterialTheme.melon.fixed.onHero,
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(accent)
                .padding(horizontal = 11.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun RowScope.LaterCard(item: OverviewTodayItem, onClick: () -> Unit) {
    RowScopeCardBase(
        item = item,
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, RoundedCornerShape(16.dp)),
        titleWeight = FontWeight.Medium,
        titleColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview
@Composable
private fun TodayTimelinePreview() {
    MelonTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            TodayTimeline(
                items = OverviewFixtures.today,
                weekdayLabel = OverviewFixtures.WEEKDAY,
                onOpenClass = {},
                onOpenSchedule = {},
            )
        }
    }
}
