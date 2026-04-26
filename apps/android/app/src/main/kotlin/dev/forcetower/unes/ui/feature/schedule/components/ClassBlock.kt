package dev.forcetower.unes.ui.feature.schedule.components

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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.schedule.ScheduleClass
import dev.forcetower.unes.ui.feature.schedule.ScheduleClassState
import dev.forcetower.unes.ui.feature.schedule.durationMin

// One row in the focused day column: a thin time rail on the left + a card
// with code chip, title, optional topic, location footer, and prof.
//
// • `Now`  — card filled with the discipline color, white text, "agora" chip.
// • `Done` — dimmed (0.52 alpha) with a check glyph next to the code chip.
// • Other — neutral card surface with the discipline color shown as a left
//   accent rail.
@Composable
internal fun ScheduleClassBlock(
    cls: ScheduleClass,
    state: ScheduleClassState,
    showGap: Boolean,
    gapMin: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (showGap && gapMin > 30) {
            GapMarker(gapMin = gapMin)
        }
        ClassRow(cls = cls, state = state)
    }
}

@Composable
private fun GapMarker(gapMin: Int) {
    val line = MaterialTheme.melon.surface.line
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val label = remember(gapMin) { formatGapLabel(gapMin) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp, end = 6.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(line),
        )
        Text(
            text = stringResource(R.string.schedule_gap_format, label).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
            ),
            color = ink4.copy(alpha = 0.7f),
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(line),
        )
    }
}

private fun formatGapLabel(gapMin: Int): String {
    val hours = gapMin / 60
    val minutes = gapMin % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h${minutes.toString().padStart(2, '0')}"
        hours > 0 -> "${hours}h"
        else -> "${minutes}min"
    }
}

@Composable
private fun ClassRow(cls: ScheduleClass, state: ScheduleClassState) {
    val isNow = state == ScheduleClassState.Now
    val isDone = state == ScheduleClassState.Done

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .alpha(if (isDone) 0.52f else 1f),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        TimeRail(start = cls.start, end = cls.end, isNow = isNow)
        ClassCard(cls = cls, state = state, isNow = isNow, isDone = isDone)
    }
}

@Composable
private fun TimeRail(start: String, end: String, isNow: Boolean) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink2 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .width(50.dp)
            .padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = start,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (isNow) ink else ink2,
        )
        Text(
            text = end,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            ),
            color = ink3.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun ClassCard(
    cls: ScheduleClass,
    state: ScheduleClassState,
    isNow: Boolean,
    isDone: Boolean,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val onAlwaysDark = Color(0xFFFBF7F2)
    val cardShape = RoundedCornerShape(20.dp)
    val cardBackground = if (isNow) cls.color else card
    val cardBorder = if (isNow) Color.Transparent else cardLine

    // `IntrinsicSize.Max` lets the accent rail share the same height as the
    // content column inside the card without measuring twice — Compose's
    // equivalent of the iOS ZStack stretching the rail to the parent's
    // measured frame. The rail itself uses `fillMaxHeight` so the union of
    // these two declares "match my sibling's height".
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .clip(cardShape)
            .background(cardBackground)
            .border(1.dp, cardBorder, cardShape),
    ) {
        if (!isNow) {
            Box(
                modifier = Modifier
                    .padding(vertical = 14.dp)
                    .padding(start = 0.dp)
                    .fillMaxHeight()
                    .width(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(cls.color.copy(alpha = if (isDone) 0.45f else 1f)),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = if (isNow) 14.dp else 15.dp,
                    end = 14.dp,
                    top = 14.dp,
                    bottom = 14.dp,
                ),
        ) {
            HeaderRow(cls = cls, isNow = isNow, isDone = isDone)

            Spacer(Modifier.height(6.dp))

            Text(
                text = cls.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 19.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.19).sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = if (isNow) onAlwaysDark else ink,
            )

            cls.topic?.let { topic ->
                Spacer(Modifier.height(3.dp))
                TopicRow(topic = topic, isNow = isNow)
            }

            Spacer(Modifier.height(9.dp))

            FooterRow(cls = cls, isNow = isNow, isDone = isDone)
        }
    }
}

@Composable
private fun HeaderRow(cls: ScheduleClass, isNow: Boolean, isDone: Boolean) {
    val onAlwaysDark = Color(0xFFFBF7F2)
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CodeChip(
            code = cls.code,
            background = if (isNow) onAlwaysDark.copy(alpha = 0.18f) else cls.color.copy(alpha = 0.10f),
            foreground = if (isNow) onAlwaysDark else cls.color,
        )
        if (isDone) {
            CheckGlyph(color = ink3.copy(alpha = 0.6f))
        }
        Spacer(Modifier.weight(1f))
        if (isNow) {
            NowChip(foreground = onAlwaysDark)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = stringResource(R.string.schedule_duration_format, cls.durationMin),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            ),
            color = if (isNow) onAlwaysDark.copy(alpha = 0.75f) else ink4,
            maxLines = 1,
        )
    }
}

@Composable
private fun CodeChip(code: String, background: Color, foreground: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.14.sp,
            ),
            color = foreground,
        )
    }
}

@Composable
private fun NowChip(foreground: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(foreground.copy(alpha = 0.22f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            PulsingDot(color = foreground)
            Text(
                text = stringResource(R.string.schedule_now_state),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.33.sp,
                ),
                color = foreground,
            )
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "now-pulse")
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
            .size(5.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun TopicRow(topic: String, isNow: Boolean) {
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val onAlwaysDark = Color(0xFFFBF7F2)
    val foreground = if (isNow) onAlwaysDark.copy(alpha = 0.85f) else ink3
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TopicLinesGlyph(color = foreground.copy(alpha = 0.7f))
        Text(
            text = stringResource(R.string.schedule_topic_format, topic),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
            ),
            color = foreground,
            maxLines = 1,
        )
    }
}

@Composable
private fun FooterRow(
    cls: ScheduleClass,
    isNow: Boolean,
    isDone: Boolean,
) {
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val onAlwaysDark = Color(0xFFFBF7F2)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScheduleLocationRow(
            cls = cls,
            style = if (isNow) LocationRowStyle.Inverted else LocationRowStyle.Normal(dim = isDone),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = cls.prof,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = if (isNow) onAlwaysDark.copy(alpha = 0.75f) else ink3,
            modifier = Modifier.widthIn(max = 110.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun CheckGlyph(color: Color) {
    Canvas(modifier = Modifier.size(12.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(
            width = 1.5f * density,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val path = Path().apply {
            moveTo(w * (2.5f / 12f), h * 0.5f)
            lineTo(w * (5f / 12f), h * (8.5f / 12f))
            lineTo(w * (9.5f / 12f), h * (3.5f / 12f))
        }
        drawPath(path = path, color = color, style = stroke)
    }
}
