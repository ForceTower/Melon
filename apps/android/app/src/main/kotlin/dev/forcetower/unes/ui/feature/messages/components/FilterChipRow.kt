package dev.forcetower.unes.ui.feature.messages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.messages.MessageFilter

// Horizontally scrolling chips that filter the inbox. Active chip inverts to
// ink-on-surface; inactive chips sit in the card color with a thin line.
@Composable
internal fun FilterChipRow(
    active: MessageFilter,
    counts: Map<MessageFilter, Int>,
    onChange: (MessageFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MessageFilter.entries.forEach { filter ->
            FilterChip(
                filter = filter,
                isActive = active == filter,
                count = counts[filter] ?: 0,
                onClick = { onChange(filter) },
            )
        }
        Spacer(Modifier.width(2.dp))
    }
}

@Composable
private fun FilterChip(
    filter: MessageFilter,
    isActive: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink2 = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine

    val bg = if (isActive) ink else card
    val border = if (isActive) ink else cardLine
    val fg = if (isActive) surface else ink2

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(filter.labelRes),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = fg,
        )
        if (count > 0) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = fg,
                modifier = Modifier.alpha(0.85f),
            )
        }
    }
}
