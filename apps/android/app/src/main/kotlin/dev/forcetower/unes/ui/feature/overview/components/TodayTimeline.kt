package dev.forcetower.unes.ui.feature.overview.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.overview.OverviewClassState
import dev.forcetower.unes.ui.feature.overview.OverviewTodayItem

@Composable
internal fun TodayTimeline(
    items: List<OverviewTodayItem>,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(24.dp))
            .padding(top = 16.dp, bottom = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.overview_today_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.22).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.overview_today_week_action),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                ChevronRightGlyph(color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(4.dp))
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            items.forEachIndexed { index, item ->
                TodayRow(item = item, isLast = index == items.lastIndex)
            }
        }
    }
}

@Composable
private fun TodayRow(item: OverviewTodayItem, isLast: Boolean) {
    val isDone = item.state == OverviewClassState.Done
    val isNow = item.state == OverviewClassState.Now
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .alpha(if (isDone) 0.5f else 1f),
    ) {
        TimeRailColumn(
            time = item.time,
            color = item.color,
            state = item.state,
            isLast = isLast,
            ink3 = ink3,
            ink4 = ink4,
        )

        Column(
            modifier = Modifier
                .padding(start = 12.dp, top = 10.dp, bottom = 12.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CodeChip(code = item.code, color = item.color)
                if (isNow) {
                    Spacer(Modifier.width(8.dp))
                    NowChip(color = accent)
                }
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.075).sp,
                ),
                color = ink,
            )
            if (item.topic != null) {
                Text(
                    text = stringResource(R.string.overview_today_topic_format, item.topic),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                    ),
                    color = ink3,
                )
            } else {
                Text(
                    text = stringResource(R.string.overview_today_room_format, item.room),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = ink4,
                )
            }
        }
    }
}

@Composable
private fun TimeRailColumn(
    time: String,
    color: Color,
    state: OverviewClassState,
    isLast: Boolean,
    ink3: Color,
    ink4: Color,
) {
    val isNow = state == OverviewClassState.Now
    val isDone = state == OverviewClassState.Done
    val line = MaterialTheme.melon.surface.line
    val surface3 = MaterialTheme.colorScheme.surfaceContainerHigh
    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .width(44.dp)
            .padding(top = 12.dp),
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = if (isNow) FontWeight.SemiBold else FontWeight.Normal,
                letterSpacing = 0.22.sp,
            ),
            color = ink3,
        )

        if (!isLast) {
            Box(
                modifier = Modifier
                    .padding(start = 22.dp, top = 32.dp)
                    .width(1.dp)
                    .height(56.dp)
                    .background(line),
            )
        }

        TimelineDot(
            color = color,
            isNow = isNow,
            isDone = isDone,
            ink4 = ink4,
            surface3 = surface3,
            surface = surface,
            modifier = Modifier.padding(start = 14.dp, top = 30.dp),
        )
    }
}

@Composable
private fun TimelineDot(
    color: Color,
    isNow: Boolean,
    isDone: Boolean,
    ink4: Color,
    surface3: Color,
    surface: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(17.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isNow) {
            Box(
                modifier = Modifier
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.13f)),
            )
        }
        val fill = when {
            isNow -> color
            isDone -> ink4
            else -> surface3
        }
        val stroke = if (isNow) color else surface
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(fill)
                .border(2.dp, stroke, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) {
                Canvas(modifier = Modifier.size(9.dp)) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(w * (2f / 9f), h * 0.5f)
                        lineTo(w * (4f / 9f), h * (6.5f / 9f))
                        lineTo(w * (7f / 9f), h * (2.5f / 9f))
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(width = 1.4f * density, cap = StrokeCap.Round),
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
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            ),
            color = color,
        )
    }
}

@Composable
private fun NowChip(color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PulsingTinyDot(color = color)
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.overview_today_state_now),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.08.sp,
            ),
            color = color,
        )
    }
}

@Composable
private fun PulsingTinyDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "now-tiny")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    Box(
        modifier = Modifier
            .size(4.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun ChevronRightGlyph(color: Color, size: Dp = 10.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = 1.5f * density, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(w * 0.3f, h * 0.2f)
            lineTo(w * 0.6f, h * 0.5f)
            lineTo(w * 0.3f, h * 0.8f)
        }
        drawPath(path, color = color, style = stroke)
    }
}
