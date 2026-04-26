package dev.forcetower.unes.ui.feature.calendar.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.calendar.CalendarEvent
import dev.forcetower.unes.ui.feature.calendar.CalendarFormat
import dev.forcetower.unes.ui.feature.calendar.CalendarMath
import dev.forcetower.unes.ui.feature.calendar.CalendarStatus
import dev.forcetower.unes.ui.feature.calendar.CountdownToken
import dev.forcetower.unes.ui.feature.calendar.color
import dev.forcetower.unes.ui.feature.calendar.meshVariant
import java.util.Locale

// "Hero" card at the top of the calendar — the next actionable event,
// rendered over a category-tinted mesh on top of the always-dark hero
// background. Carries a giant countdown number and an optional progress
// bar for active windows. Mirrors `CalHero` in `screens-calendar.jsx` +
// iOS `CalHeroCard`.
//
// Foreground type is pinned (cream / always-light) — the card is dark in
// both themes, same as iOS.
private val HeroLight = Color(0xFFFBF7F2)

@Composable
internal fun CalHeroCard(event: CalendarEvent, modifier: Modifier = Modifier) {
    val category = remember(event) { CalendarMath.categorize(event) }
    val status = remember(event) { CalendarMath.status(event) }
    val isActive = status == CalendarStatus.Active
    val accent = category.color()
    val darkBg = MaterialTheme.melon.brand.alwaysDarkBg

    val progress: Float? = remember(event) {
        if (!isActive || event.end == null) null
        else {
            val total = (CalendarMath.daysBetween(event.start, event.end) + 1).coerceAtLeast(1)
            val elapsed = CalendarMath.daysBetween(event.start, CalendarMath.today) + 1
            (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 18.dp, shape = RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(darkBg),
    ) {
        Mesh(
            variant = category.meshVariant(),
            intensity = 1.05f,
            modifier = Modifier.fillMaxSize(),
        )
        // Vertical wash darkens the bottom so countdown type stays legible.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to darkBg.copy(alpha = 0.12f),
                        1f to darkBg.copy(alpha = 0.55f),
                    ),
                ),
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            TopRow(event = event, accent = accent, isActive = isActive)
            Spacer(modifier = Modifier.height(16.dp))
            EyebrowRow(event = event, category = category, accent = accent)
            Spacer(modifier = Modifier.height(10.dp))
            Title(event = event)
            Spacer(modifier = Modifier.height(18.dp))
            CountdownRow(event = event)
            if (progress != null) {
                Spacer(modifier = Modifier.height(14.dp))
                ProgressStrip(progress = progress, end = event.end, accent = accent)
            }
        }
    }
}

@Composable
private fun TopRow(event: CalendarEvent, accent: Color, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PulseDot(accent = accent, animate = isActive)
            Text(
                text = stringResource(
                    if (isActive) R.string.calendar_hero_eyebrow_now
                    else R.string.calendar_hero_eyebrow_next,
                ).uppercase(Locale.ROOT),
                style = monoStyle(10f).copy(letterSpacing = 1.8.sp),
                color = HeroLight.copy(alpha = 0.75f),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = CalendarFormat.dateRange(event.start, event.end).uppercase(Locale.ROOT),
            style = monoStyle(10f).copy(letterSpacing = 0.8.sp),
            color = HeroLight.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun EyebrowRow(
    event: CalendarEvent,
    category: dev.forcetower.unes.ui.feature.calendar.CalendarCategory,
    accent: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CalCategoryGlyph(category = category, color = accent, size = 13.dp)
        Text(
            text = (stringResource(category.labelRes) + " · " + stringResource(event.scope.labelRes))
                .uppercase(Locale.ROOT),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 14.sp,
                lineHeight = 14.sp,
                letterSpacing = 1.68.sp,
            ),
            color = accent,
        )
    }
}

@Composable
private fun Title(event: CalendarEvent) {
    Text(
        text = event.displayDescription,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 26.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.39).sp,
        ),
        color = HeroLight,
    )
}

@Composable
private fun CountdownRow(event: CalendarEvent) {
    val parts = remember(event) { CalendarMath.countdownParts(event) }
    val numberLabel = when (val n = parts.number) {
        CountdownToken.Today -> stringResource(R.string.calendar_countdown_today)
        CountdownToken.Tomorrow -> stringResource(R.string.calendar_countdown_tomorrow)
        is CountdownToken.Number -> n.value.toString()
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = numberLabel,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 48.sp,
                lineHeight = 48.sp,
                letterSpacing = (-1.44).sp,
            ),
            color = HeroLight,
        )
        if (parts.tailRes != null) {
            Text(
                text = stringResource(parts.tailRes),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontStyle = FontStyle.Italic,
                ),
                color = HeroLight.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
    }
}

@Composable
private fun ProgressStrip(progress: Float, end: java.time.LocalDate?, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(HeroLight.copy(alpha = 0.15f)),
        ) {
            val maxPx = with(LocalDensity.current) { maxWidth.toPx() }
            val widthDp = with(LocalDensity.current) { (maxPx * progress).toDp() }
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
                    .size(width = widthDp, height = 3.dp),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(
                    R.string.calendar_hero_progress_elapsed_format,
                    (progress * 100f).toInt(),
                ).uppercase(Locale.ROOT),
                style = monoStyle(9.5f).copy(letterSpacing = 0.95.sp),
                color = HeroLight.copy(alpha = 0.55f),
            )
            Spacer(modifier = Modifier.weight(1f))
            if (end != null) {
                Text(
                    text = stringResource(
                        R.string.calendar_hero_progress_closes_format,
                        CalendarFormat.dateShort(end),
                    ).uppercase(Locale.ROOT),
                    style = monoStyle(9.5f).copy(letterSpacing = 0.95.sp),
                    color = HeroLight.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun PulseDot(accent: Color, animate: Boolean) {
    val alpha = if (animate) {
        val transition = rememberInfiniteTransition(label = "hero-pulse")
        val a by transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "hero-alpha",
        )
        a
    } else 1f
    Box(
        modifier = Modifier.size(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.2f * alpha)),
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = alpha)),
        )
    }
}

@Composable
private fun monoStyle(sizeSp: Float, weight: FontWeight = FontWeight.Normal) =
    MaterialTheme.typography.labelSmall.copy(
        fontSize = sizeSp.sp,
        fontWeight = weight,
        fontFamily = FontFamily.Monospace,
    )
