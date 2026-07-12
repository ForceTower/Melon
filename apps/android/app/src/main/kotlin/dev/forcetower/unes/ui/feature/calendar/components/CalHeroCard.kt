package dev.forcetower.unes.ui.feature.calendar.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.calendar.CalendarEvent
import dev.forcetower.unes.ui.feature.calendar.CalendarFormat
import dev.forcetower.unes.ui.feature.calendar.CalendarMath
import dev.forcetower.unes.ui.feature.calendar.CalendarStatus
import dev.forcetower.unes.ui.feature.calendar.CountdownToken
import dev.forcetower.unes.ui.feature.calendar.color
import dev.forcetower.unes.ui.feature.calendar.icon

// "Próxima ação" hero — the next actionable event over the warm brand mesh on
// an always-dark plate. Status row + category·scope chip + title + giant
// countdown, plus a progress strip while an active window is running. Tapping
// opens the event sheet. Mirrors the dc `CalendarScreen` hero.
@Composable
internal fun CalHeroCard(
    event: CalendarEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = remember(event) { CalendarMath.categorize(event) }
    val isActive = remember(event) { CalendarMath.status(event) == CalendarStatus.Active }
    val progress = remember(event) { CalendarMath.progress(event) }
    val accent = category.color()
    val plate = MaterialTheme.melon.brand.alwaysDarkBg
    val veil = MaterialTheme.melon.fixed.nightVeil
    val onHero = MaterialTheme.melon.fixed.onHero
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 14.dp, shape = shape)
            .clip(shape)
            .background(plate)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Mesh(
            variant = MeshVariant.Warm,
            intensity = 1.05f,
            modifier = Modifier.fillMaxSize(),
        )
        // Vertical wash darkens the bottom so countdown type stays legible.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to veil.copy(alpha = 0.14f),
                        1f to veil.copy(alpha = 0.66f),
                    ),
                ),
        )

        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp)) {
            StatusRow(event = event, accent = accent, isActive = isActive, onHero = onHero)
            Spacer(modifier = Modifier.height(15.dp))
            CategoryChip(event = event, accent = accent, onHero = onHero)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = event.displayDescription,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.44).sp,
                ),
                color = onHero,
            )
            Spacer(modifier = Modifier.height(14.dp))
            CountdownRow(event = event, onHero = onHero)
            if (progress != null) {
                Spacer(modifier = Modifier.height(16.dp))
                ProgressStrip(progress = progress, end = event.end, accent = accent, onHero = onHero)
            }
        }
    }
}

@Composable
private fun StatusRow(event: CalendarEvent, accent: Color, isActive: Boolean, onHero: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PulseDot(accent = accent, animate = isActive)
            Text(
                text = stringResource(
                    if (isActive) R.string.calendar_hero_eyebrow_now
                    else R.string.calendar_hero_eyebrow_next,
                ),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = onHero.copy(alpha = 0.9f),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = CalendarFormat.dateRange(event.start, event.end),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = onHero.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun CategoryChip(event: CalendarEvent, accent: Color, onHero: Color) {
    val category = remember(event) { CalendarMath.categorize(event) }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(onHero.copy(alpha = 0.13f))
            .padding(start = 6.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = category.icon(),
                contentDescription = null,
                tint = onHero,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = stringResource(category.labelRes) + " · " + stringResource(event.scope.labelRes),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = onHero,
        )
    }
}

@Composable
private fun CountdownRow(event: CalendarEvent, onHero: Color) {
    val parts = remember(event) { CalendarMath.countdownParts(event) }
    val numberLabel = when (val n = parts.number) {
        CountdownToken.Today -> stringResource(R.string.calendar_countdown_today)
        CountdownToken.Tomorrow -> stringResource(R.string.calendar_countdown_tomorrow)
        is CountdownToken.Number -> n.value.toString()
    }
    // Word tokens ("hoje", "amanhã") drop to a smaller size, same as the dc.
    val numberSize = if (numberLabel.length > 3) 34.sp else 52.sp
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = numberLabel,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = numberSize,
                lineHeight = numberSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.5).sp,
            ),
            color = onHero,
        )
        if (parts.tailRes != null) {
            Text(
                text = stringResource(parts.tailRes),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = onHero.copy(alpha = 0.82f),
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun ProgressStrip(
    progress: Float,
    end: java.time.LocalDate?,
    accent: Color,
    onHero: Color,
) {
    val fill = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        fill.animateTo(progress, tween(durationMillis = 900, easing = MelonMotion.EmphasizedEasing))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(onHero.copy(alpha = 0.16f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill.value)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(
                    R.string.calendar_hero_progress_elapsed_format,
                    (progress * 100f).toInt(),
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = onHero.copy(alpha = 0.62f),
            )
            Spacer(modifier = Modifier.weight(1f))
            if (end != null) {
                Text(
                    text = stringResource(
                        R.string.calendar_hero_progress_closes_format,
                        CalendarFormat.dateShort(end),
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = onHero.copy(alpha = 0.62f),
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
            initialValue = 0.4f,
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
        modifier = Modifier.size(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.25f * alpha)),
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = alpha)),
        )
    }
}
