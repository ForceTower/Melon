package dev.forcetower.unes.ui.feature.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.calendar.CalendarCategoryFilter
import dev.forcetower.unes.ui.feature.calendar.CalendarScopeFilter

// Two horizontally-scrolling chip rows — one for category (Tudo / Prazos /
// Provas / Feriados), one slightly smaller for scope. Both share `CalChip`.
@Composable
internal fun CalCategoryFilterRow(
    active: CalendarCategoryFilter,
    onChange: (CalendarCategoryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarCategoryFilter.entries.forEach { f ->
            CalChip(
                label = stringResource(f.labelRes),
                isActive = active == f,
                small = false,
                onClick = { onChange(f) },
            )
        }
    }
}

@Composable
internal fun CalScopeFilterRow(
    active: CalendarScopeFilter,
    onChange: (CalendarScopeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarScopeFilter.entries.forEach { f ->
            CalChip(
                label = stringResource(f.labelRes),
                isActive = active == f,
                small = true,
                onClick = { onChange(f) },
            )
        }
    }
}

@Composable
private fun CalChip(label: String, isActive: Boolean, small: Boolean, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val outline = MaterialTheme.melon.surface.line
    val surfaceFg = MaterialTheme.colorScheme.surface
    val ink2 = MaterialTheme.colorScheme.onSurfaceVariant

    val background = if (isActive) ink else Color.Transparent
    val borderColor = if (isActive) ink else outline
    val foreground = if (isActive) surfaceFg else ink2

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = if (small) 10.dp else 13.dp, vertical = if (small) 6.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = if (small) 11.sp else 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.06).sp,
            ),
            color = foreground,
        )
    }
}
