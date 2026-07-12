package dev.forcetower.unes.ui.feature.schedule.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

// M3 week date rail (dc `UNES Horário - Android`): a tonal rounded container
// with the 7 weekday columns — single-letter weekday, date circle, and up to
// four class-count dots. The selected day fills with the accent; today (when
// not selected) gets an accent ring.
@Composable
internal fun ScheduleWeekRail(
    activeIdx: Int,
    todayIdx: Int,
    dates: List<Int>,
    counts: List<Int>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Monday-anchored, matching the rail's 0..6 indexing. Narrow style gives
    // the single letter in the device locale (pt "S T Q Q S S D").
    val letters = remember {
        DayOfWeek.entries.map {
            it.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                .uppercase(Locale.getDefault())
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 10.dp),
    ) {
        for (i in 0 until 7) {
            RailDay(
                letter = letters.getOrNull(i).orEmpty(),
                date = dates.getOrNull(i) ?: 0,
                count = counts.getOrNull(i) ?: 0,
                isSelected = i == activeIdx,
                isToday = i == todayIdx,
                onClick = { onSelect(i) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RailDay(
    letter: String,
    date: Int,
    count: Int,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val ink2 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val interaction = remember { MutableInteractionSource() }

    val circleBackground by animateColorAsState(
        targetValue = if (isSelected) accent else Color.Transparent,
        animationSpec = tween(200),
        label = "rail-circle-bg",
    )
    val circleForeground by animateColorAsState(
        targetValue = when {
            isSelected -> onAccent
            isToday -> accent
            else -> ink2
        },
        animationSpec = tween(200),
        label = "rail-circle-fg",
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .semantics {
                role = Role.Tab
                selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.48.sp,
            ),
            color = if (isSelected || isToday) accent else ink4,
        )
        Box(
            modifier = Modifier
                .then(
                    if (isSelected) {
                        Modifier.shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            ambientColor = accent,
                            spotColor = accent,
                        )
                    } else {
                        Modifier
                    },
                )
                .size(34.dp)
                .clip(CircleShape)
                .background(circleBackground)
                .then(
                    if (isToday && !isSelected) {
                        Modifier.border(2.dp, accent, CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = circleForeground,
            )
        }
        Row(
            modifier = Modifier.height(5.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(count.coerceAtMost(4)) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) accent else ink4.copy(alpha = 0.6f)),
                )
            }
        }
    }
}
