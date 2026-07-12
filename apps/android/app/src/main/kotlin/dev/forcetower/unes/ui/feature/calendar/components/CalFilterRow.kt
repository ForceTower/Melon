package dev.forcetower.unes.ui.feature.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.calendar.CalendarCategory
import dev.forcetower.unes.ui.feature.calendar.CalendarCategoryFilter
import dev.forcetower.unes.ui.feature.calendar.CalendarScopeFilter
import dev.forcetower.unes.ui.feature.calendar.color

// Full-width segmented control for the category filter — tonal track with a
// raised card thumb, each category option carrying its color dot. Same pill
// recipe as `GroupSegmented` on the discipline detail. Mirrors the dc
// segmented row.
@Composable
internal fun CalCategorySegmented(
    active: CalendarCategoryFilter,
    onChange: (CalendarCategoryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        CalendarCategoryFilter.entries.forEach { filter ->
            SegmentButton(
                filter = filter,
                isActive = active == filter,
                onClick = { onChange(filter) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SegmentButton(
    filter: CalendarCategoryFilter,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dotColor = when (filter) {
        CalendarCategoryFilter.All -> null
        CalendarCategoryFilter.Deadline -> CalendarCategory.Deadline.color()
        CalendarCategoryFilter.Exam -> CalendarCategory.Exam.color()
        CalendarCategoryFilter.Holiday -> CalendarCategory.Holiday.color()
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        color = if (isActive) MaterialTheme.melon.surface.card else androidx.compose.ui.graphics.Color.Transparent,
        shadowElevation = if (isActive) 1.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(dotColor),
                )
            }
            Text(
                text = stringResource(filter.labelRes),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.12).sp,
                ),
                color = if (isActive) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

// Horizontally-scrolling scope pills: active = filled ink, inactive = outlined
// card. Mirrors the dc scope chips.
@Composable
internal fun CalScopeChips(
    active: CalendarScopeFilter,
    onChange: (CalendarScopeFilter) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp = 20.dp,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarScopeFilter.entries.forEach { filter ->
            val isActive = active == filter
            Surface(
                onClick = { onChange(filter) },
                shape = CircleShape,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.melon.surface.card
                },
                border = if (isActive) null else BorderStroke(1.dp, MaterialTheme.melon.surface.line),
            ) {
                Text(
                    text = stringResource(filter.labelRes),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (isActive) {
                        MaterialTheme.colorScheme.background
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}
