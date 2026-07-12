package dev.forcetower.unes.ui.feature.campusevent

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventActivity
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventAudience
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventCategory
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.melon
import java.time.ZoneId
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay

// Per-second wall clock, alive only while a campus-event screen composes it —
// the Compose analogue of iOS `TimelineView(.periodic(by: 1))`. Drives the
// countdowns, "AGORA" badges and phase flips.
@Composable
internal fun rememberCampusEventNow(): Instant {
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now()
            delay(1_000L)
        }
    }
    return now
}

// MARK: Tone mapping — mirrors iOS `CampusEventComponents.swift`, routed
// through the theme instead of raw hexes.

@get:StringRes
internal val CampusEventCategory.label: Int
    get() = when (this) {
        CampusEventCategory.Quest -> R.string.campus_event_category_quest
        CampusEventCategory.Workshop -> R.string.campus_event_category_workshop
        CampusEventCategory.Lecture -> R.string.campus_event_category_lecture
        CampusEventCategory.Presentation -> R.string.campus_event_category_presentation
        CampusEventCategory.GroupDynamic -> R.string.campus_event_category_dynamic
        CampusEventCategory.Other -> R.string.campus_event_category_other
    }

internal val CampusEventCategory.icon: ImageVector
    get() = when (this) {
        CampusEventCategory.Quest -> Icons.Outlined.AutoAwesome
        CampusEventCategory.Workshop -> Icons.Outlined.Build
        CampusEventCategory.Lecture -> Icons.Outlined.Mic
        CampusEventCategory.Presentation -> Icons.Rounded.Star
        CampusEventCategory.GroupDynamic -> Icons.Outlined.Group
        CampusEventCategory.Other -> Icons.Outlined.GridView
    }

internal val CampusEventCategory.mesh: MeshVariant
    get() = when (this) {
        CampusEventCategory.Quest -> MeshVariant.Sun
        CampusEventCategory.Workshop -> MeshVariant.Cool
        CampusEventCategory.Lecture, CampusEventCategory.Presentation -> MeshVariant.Rose
        CampusEventCategory.GroupDynamic, CampusEventCategory.Other -> MeshVariant.Warm
    }

@Composable
internal fun CampusEventCategory.tone(): Color {
    val palette = MaterialTheme.melon.palette
    return when (this) {
        CampusEventCategory.Quest -> palette.amber
        CampusEventCategory.Workshop -> palette.teal
        CampusEventCategory.Lecture -> palette.violet
        CampusEventCategory.Presentation -> palette.magenta
        CampusEventCategory.GroupDynamic -> palette.coral
        CampusEventCategory.Other -> palette.orange
    }
}

@get:StringRes
internal val CampusEventAudience.label: Int
    get() = when (this) {
        CampusEventAudience.Everyone -> R.string.campus_event_filter_everyone
        CampusEventAudience.Freshmen -> R.string.campus_event_filter_freshmen
        CampusEventAudience.Veterans -> R.string.campus_event_filter_veterans
    }

@Composable
internal fun CampusEventAudience.tone(): Color {
    val palette = MaterialTheme.melon.palette
    return when (this) {
        CampusEventAudience.Everyone -> palette.green
        CampusEventAudience.Freshmen -> palette.coral
        CampusEventAudience.Veterans -> palette.teal
    }
}

// Deterministic per-name accent, stable across launches — drives avatar
// gradients and venue/organization list tints.
internal fun campusEventStableIndex(text: String, count: Int): Int {
    if (count <= 0) return 0
    var hash = 0u
    for (char in text) {
        hash = hash * 31u + char.code.toUInt()
    }
    return (hash % count.toUInt()).toInt()
}

// The rotating tint palette for venues and organizations.
@Composable
internal fun campusEventPalette(): List<Color> {
    val palette = MaterialTheme.melon.palette
    return listOf(
        palette.violet, palette.teal, palette.amber,
        palette.magenta, palette.green, palette.coral,
    )
}

@Composable
internal fun campusEventTone(text: String): Color {
    val palette = campusEventPalette()
    return palette[campusEventStableIndex(text, palette.size)]
}

// MARK: Card chrome

// The standard card: card fill, hairline border, soft shadow.
@Composable
internal fun Modifier.campusEventCard(cornerRadius: Dp = 20.dp): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(elevation = 2.dp, shape = shape, clip = false)
        .clip(shape)
        .background(MaterialTheme.melon.surface.card)
        .border(1.dp, MaterialTheme.melon.surface.cardLine, shape)
}

// Section title with an optional supporting note, aligned with the cards.
@Composable
internal fun CampusEventSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// MARK: Chips & tiles

// Audience capsule: tinted dot + label.
@Composable
internal fun CampusEventAudienceChip(
    audience: CampusEventAudience,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val tone = audience.tone()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(tone.copy(alpha = 0.12f))
            .padding(
                horizontal = if (large) 10.dp else 8.dp,
                vertical = if (large) 5.dp else 3.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(tone),
        )
        Text(
            text = stringResource(audience.label),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (large) 12.5.sp else 11.sp,
                letterSpacing = 0.sp,
            ),
            color = tone,
            maxLines = 1,
        )
    }
}

// Category badge: icon + label on a tinted rounded rect.
@Composable
internal fun CampusEventCategoryPill(
    category: CampusEventCategory,
    modifier: Modifier = Modifier,
) {
    val tone = category.tone()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tone.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = stringResource(category.label),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
            color = tone,
            maxLines = 1,
        )
    }
}

// Rounded square glyph carrying the category color.
@Composable
internal fun CampusEventCategoryTile(
    category: CampusEventCategory,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val tone = category.tone()
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(size * 0.28f), clip = false)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(tone),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = MaterialTheme.melon.fixed.onHero,
            modifier = Modifier.size(size * 0.44f),
        )
    }
}

// Initials over a gradient picked by a stable hash of the name.
@Composable
internal fun CampusEventAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
) {
    val palette = MaterialTheme.melon.palette
    val gradients = listOf(
        palette.coral to palette.amber,
        palette.violet to palette.magenta,
        palette.teal to palette.green,
        palette.magenta to palette.coral,
        palette.teal to palette.violet,
        palette.amber to palette.coral,
    )
    val pair = gradients[campusEventStableIndex(name, gradients.size)]
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(pair.first, pair.second))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = campusEventInitials(name),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = (size.value * 0.38f).sp),
            color = MaterialTheme.melon.fixed.onHero,
        )
    }
}

internal fun campusEventInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val first = parts.firstOrNull() ?: return "•"
    if (parts.size == 1) return first.take(2).uppercase()
    return "${first.take(1)}${parts.last().take(1)}".uppercase()
}

// Faint tinted radial washing down from behind the top bar on detail screens.
@Composable
internal fun CampusEventDetailWash(tone: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .offset(y = (-60).dp)
            .graphicsLayer { clip = false }
            .background(
                Brush.radialGradient(
                    colors = listOf(tone.copy(alpha = 0.26f), Color.Transparent),
                    radius = 900f,
                ),
            ),
    )
}

// Pulsing live dot — the design's `siePulse` keyframes.
@Composable
internal fun CampusEventLiveDot(
    modifier: Modifier = Modifier,
    size: Dp = 7.dp,
    color: Color = MaterialTheme.melon.fixed.live,
) {
    val transition = rememberInfiniteTransition(label = "campus-live-pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(color),
    )
}

// MARK: Countdown

// One D/H/M/S cell of the big countdown.
@Composable
private fun CampusEventCountCell(value: Int, label: String) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Column(
        modifier = Modifier.widthIn(min = 46.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = CampusEventFormat.padded(value),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
            color = onHero,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = onHero.copy(alpha = 0.6f),
        )
    }
}

// The full "DD : HH : MM : SS" row, rendered for the ticking `now`.
@Composable
internal fun CampusEventCountdownRow(
    target: Instant,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    val countdown = CampusEventFormat.countdown(target, now)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CampusEventCountCell(countdown.days, stringResource(R.string.campus_event_countdown_days))
        CountSeparator()
        CampusEventCountCell(countdown.hours, stringResource(R.string.campus_event_countdown_hours))
        CountSeparator()
        CampusEventCountCell(countdown.minutes, stringResource(R.string.campus_event_countdown_minutes))
        CountSeparator()
        CampusEventCountCell(countdown.seconds, stringResource(R.string.campus_event_countdown_seconds))
    }
}

@Composable
private fun CountSeparator() {
    Text(
        text = ":",
        style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp),
        color = MaterialTheme.melon.fixed.onHero.copy(alpha = 0.32f),
        modifier = Modifier.padding(top = 1.dp),
    )
}

// MARK: Hero bits (on-dark)

// Compact activity block inside the hero footer: category tile + tagged
// title + time/venue meta.
@Composable
internal fun CampusEventHeroActivityBlock(
    activity: CampusEventActivity,
    tag: String,
    zone: ZoneId,
    modifier: Modifier = Modifier,
    tagTone: Color = MaterialTheme.melon.fixed.onHero.copy(alpha = 0.55f),
) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        CampusEventCategoryTile(category = activity.category, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tag.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = tagTone,
            )
            Text(
                text = activity.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = onHero,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
            Text(
                text = listOf(
                    CampusEventFormat.weekdayShort(activity.startsAt, zone),
                    CampusEventFormat.time(activity.startsAt, zone),
                    activity.venueName,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = onHero.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

// Per-day progress segments while the event runs: done days in white, the
// current one in live green.
@Composable
internal fun CampusEventDayProgress(
    dayCount: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    val onHero = MaterialTheme.melon.fixed.onHero
    val live = MaterialTheme.melon.fixed.live
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(maxOf(dayCount, 1)) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        when {
                            index < currentIndex -> onHero.copy(alpha = 0.9f)
                            index == currentIndex -> live
                            else -> onHero.copy(alpha = 0.22f)
                        },
                    ),
            )
        }
    }
}

// Number + label stat in the ended hero.
@Composable
internal fun CampusEventHeroStat(value: Int, label: String, modifier: Modifier = Modifier) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = onHero,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
            color = onHero.copy(alpha = 0.55f),
        )
    }
}

// Row separator inside grouped cards, indented past the leading tile.
@Composable
internal fun CampusEventRowDivider(startIndent: Dp, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.width(startIndent))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.melon.surface.line),
        )
    }
}
