package dev.forcetower.unes.ui.feature.commute

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitArrivalState
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitRoute
import dev.forcetower.melon.feature.transit.domain.model.TransitStop
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// The vocabulary every Meu trajeto screen shares (iOS `CommuteComponents`):
// the line badge, the state chip, the freshness stamp, the big estimate, the
// straight route line, the card plate and the section header.

// Tabular figures so a ticking estimate doesn't jitter.
private const val TABULAR = "tnum"

// ───────── Line badge ─────────

internal enum class TransitBadgeSize { Small, Regular, Large }

@Composable
internal fun TransitLineBadge(
    number: String,
    modifier: Modifier = Modifier,
    size: TransitBadgeSize = TransitBadgeSize.Regular,
    // Tinted outline instead of a solid fill — an unselected line.
    ghost: Boolean = false,
) {
    val color = MaterialTheme.melon.transit.line(number)
    val (fontSize, minWidth, height, radius) = when (size) {
        TransitBadgeSize.Small -> BadgeMetrics(10.5.sp, 30.dp, 19.dp, 5.dp)
        TransitBadgeSize.Regular -> BadgeMetrics(13.sp, 40.dp, 25.dp, 7.dp)
        TransitBadgeSize.Large -> BadgeMetrics(17.sp, 50.dp, 32.dp, 9.dp)
    }
    val description = stringResource(R.string.commute_line_accessibility, number)
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = minWidth, minHeight = height)
            .clip(RoundedCornerShape(radius))
            .background(if (ghost) color.copy(alpha = 0.15f) else color)
            .padding(horizontal = if (size == TransitBadgeSize.Small) 5.dp else 7.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                fontFeatureSettings = TABULAR,
            ),
            color = if (ghost) color else MaterialTheme.melon.fixed.onHero,
            maxLines = 1,
        )
    }
}

private data class BadgeMetrics(
    val fontSize: TextUnit,
    val minWidth: Dp,
    val height: Dp,
    val radius: Dp,
)

// ───────── State chip ─────────

// Every state carries a symbol and a label; color only reinforces it.
internal enum class TransitChipKind { Live, Waiting, Historical, Stale, Flaky }

internal fun transitChipKind(arrival: TransitArrival): TransitChipKind =
    when {
        arrival.signalLost -> TransitChipKind.Stale
        arrival.state == TransitArrivalState.Live -> TransitChipKind.Live
        arrival.state == TransitArrivalState.Waiting -> TransitChipKind.Waiting
        else -> TransitChipKind.Historical
    }

@Composable
internal fun TransitChip(
    kind: TransitChipKind,
    modifier: Modifier = Modifier,
    text: String? = null,
    small: Boolean = false,
    onDark: Boolean = false,
) {
    val tone = when (kind) {
        TransitChipKind.Live -> MaterialTheme.colorScheme.primary
        TransitChipKind.Waiting -> MaterialTheme.melon.transit.waiting
        TransitChipKind.Historical, TransitChipKind.Flaky -> MaterialTheme.colorScheme.outline
        TransitChipKind.Stale -> MaterialTheme.colorScheme.outlineVariant
    }
    val white = MaterialTheme.melon.fixed.onHero
    val content = if (onDark) white else tone
    val fill = when {
        onDark -> white.copy(alpha = 0.14f)
        kind == TransitChipKind.Live || kind == TransitChipKind.Waiting -> tone.copy(alpha = 0.12f)
        kind == TransitChipKind.Flaky -> Color.Transparent
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val outline = if (onDark) white.copy(alpha = 0.22f) else tone.copy(alpha = 0.3f)
    val label = text ?: stringResource(
        when (kind) {
            TransitChipKind.Live -> R.string.commute_state_live
            TransitChipKind.Waiting -> R.string.commute_state_waiting
            TransitChipKind.Historical -> R.string.commute_state_historical
            TransitChipKind.Stale -> R.string.commute_state_stale
            TransitChipKind.Flaky -> R.string.commute_state_flaky
        },
    )
    val shape = CircleShape
    Row(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .then(
                if (kind == TransitChipKind.Flaky) {
                    Modifier.dashedBorder(content)
                } else {
                    Modifier.border(1.dp, outline, shape)
                },
            )
            .padding(
                start = if (small) 6.dp else 7.dp,
                end = if (small) 7.dp else 9.dp,
                top = if (small) 2.dp else 3.dp,
                bottom = if (small) 2.dp else 3.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (small) 4.dp else 5.dp),
    ) {
        ChipSymbol(kind = kind, tint = content, size = if (small) 11.dp else 12.dp)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = if (small) 10.5.sp else 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            ),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ChipSymbol(
    kind: TransitChipKind,
    tint: Color,
    size: Dp,
) {
    val icon = when (kind) {
        TransitChipKind.Live -> Icons.Filled.Sensors
        TransitChipKind.Waiting -> Icons.Filled.LocalParking
        TransitChipKind.Historical -> Icons.Filled.History
        TransitChipKind.Stale -> Icons.Filled.SensorsOff
        TransitChipKind.Flaky -> Icons.AutoMirrored.Outlined.HelpOutline
    }
    // The live symbol pulses, like the iOS `.symbolEffect(.pulse)`.
    val alpha = if (kind == TransitChipKind.Live) {
        val transition = rememberInfiniteTransition(label = "chip-pulse")
        val pulse by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse",
        )
        pulse
    } else {
        1f
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(size)
            .alpha(alpha),
    )
}

private fun Modifier.dashedBorder(color: Color): Modifier =
    drawBehind {
        val stroke = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())),
        )
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(size.height / 2f),
            style = stroke,
        )
    }

// ───────── Freshness ─────────

// How long ago the bus last reported a new position.
@Composable
internal fun TransitFreshness(
    fixAt: Instant,
    now: Instant,
    modifier: Modifier = Modifier,
    signalLost: Boolean = false,
    onDark: Boolean = false,
    short: Boolean = false,
) {
    val white = MaterialTheme.melon.fixed.onHero
    val color = when {
        onDark -> white.copy(alpha = if (signalLost) 0.75f else 0.62f)
        signalLost -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (signalLost) {
            Icon(
                imageVector = Icons.Filled.SensorsOff,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(11.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = color.alpha * 0.8f)),
            )
        }
        Text(
            text = if (signalLost) signalLostLabel(fixAt, now) else freshnessLabel(fixAt, now, short),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                fontFeatureSettings = TABULAR,
            ),
            color = color,
            maxLines = 1,
        )
    }
}

// ───────── Estimate ─────────

// The number that matters. Approximate times get a smaller, dimmer "~".
@Composable
internal fun TransitEstimateText(
    estimate: CommuteEstimate,
    modifier: Modifier = Modifier,
    dim: Boolean = false,
    stale: Boolean = false,
    size: TextUnit = 30.sp,
    onDark: Boolean = false,
) {
    val color = when {
        onDark -> MaterialTheme.melon.fixed.onHero
        dim -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onBackground
    }
    val weight = if (dim) FontWeight.SemiBold else FontWeight.Bold
    Row(
        modifier = modifier.alpha(if (stale) 0.45f else 1f),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (estimate.isApproximate) {
            Text(
                text = stringResource(R.string.commute_approx_prefix),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = size * 0.72f,
                    lineHeight = size,
                    fontWeight = weight,
                    letterSpacing = 0.sp,
                ),
                color = color.copy(alpha = 0.55f),
                maxLines = 1,
            )
        }
        Text(
            text = estimate.label(),
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = size,
                lineHeight = size,
                fontWeight = weight,
                letterSpacing = -(size * 0.035f),
                fontFeatureSettings = TABULAR,
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ───────── Route progress ─────────

// The route as a straight line from its origin to the end, the bus on it and
// the student's stop ringed.
@Composable
internal fun TransitRouteProgress(
    stopNames: List<String>,
    // Fractional stop index; null when there is no bus to place.
    busPosition: Double?,
    mineIndex: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
) {
    val white = MaterialTheme.melon.fixed.onHero
    val track = if (onDark) white.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh
    val ring = if (onDark) white else MaterialTheme.colorScheme.onBackground
    val labelColor = if (onDark) white.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
    val markerBorder = if (onDark) MaterialTheme.melon.fixed.heroVeil else MaterialTheme.melon.surface.card
    val yourStop = stringResource(R.string.commute_route_your_stop)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    )
    val last = (stopNames.size - 1).coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .drawBehind {
                    val inset = size.width * 0.08f
                    val span = size.width - inset * 2
                    val y = size.height / 2f
                    val x = { index: Double -> inset + span * (index / last).toFloat() }
                    val strokeWidth = 3.dp.toPx()
                    drawLine(track, Offset(inset, y), Offset(inset + span, y), strokeWidth, StrokeCap.Round)
                    if (busPosition != null) {
                        drawLine(color, Offset(inset, y), Offset(x(busPosition), y), strokeWidth, StrokeCap.Round)
                    }
                    stopNames.indices.forEach { index ->
                        val passed = busPosition != null && index <= busPosition
                        val mine = index == mineIndex
                        val radius = when {
                            mine -> 5.5.dp.toPx()
                            stopNames.size > 16 -> 2.dp.toPx()
                            else -> 3.dp.toPx()
                        }
                        drawCircle(if (passed) color else track, radius, Offset(x(index.toDouble()), y))
                        if (mine) {
                            drawCircle(
                                ring,
                                radius,
                                Offset(x(index.toDouble()), y),
                                style = Stroke(2.5.dp.toPx()),
                            )
                        }
                    }
                },
        ) {
            if (busPosition != null) {
                val fraction = (busPosition / last).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                ) {
                    BusMarker(
                        color = color,
                        border = markerBorder,
                        fraction = 0.08f + 0.84f * fraction,
                    )
                }
            }
        }
        // "seu ponto" sits under its dot; the end labels give way to it
        // rather than print over it.
        RouteLabels(
            first = stopNames.firstOrNull(),
            last = stopNames.lastOrNull()?.takeIf { stopNames.size > 1 },
            mineFraction = (mineIndex.toDouble() / last).toFloat(),
        ) {
            Text(text = yourStop, style = labelStyle.copy(fontWeight = FontWeight.Bold), color = ring, maxLines = 1)
            Text(text = stopNames.firstOrNull().orEmpty(), style = labelStyle, color = labelColor, maxLines = 1)
            Text(text = stopNames.lastOrNull().orEmpty(), style = labelStyle, color = labelColor, maxLines = 1)
        }
    }
}

// Places the three labels (yours, the origin, the end) at their real x along
// the track. The origin and end labels are dropped whenever they would
// overlap yours, which keeps a stop near the campus readable.
@Composable
private fun RouteLabels(
    first: String?,
    last: String?,
    mineFraction: Float,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = Modifier.fillMaxWidth()) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val mine = measurables[0].measure(loose)
        val firstLabel = measurables[1].measure(loose)
        val lastLabel = measurables[2].measure(loose)
        val width = constraints.maxWidth
        val inset = width * 0.08f
        val span = width - inset * 2
        val gap = 8.dp.roundToPx()
        val mineX = (inset + span * mineFraction - mine.width / 2f).roundToInt().coerceIn(0, width - mine.width)
        val firstX = (inset - 4.dp.toPx()).roundToInt().coerceAtLeast(0)
        val lastX = (inset + span + 4.dp.toPx() - lastLabel.width).roundToInt().coerceAtMost(width - lastLabel.width)
        val height = maxOf(mine.height, firstLabel.height, lastLabel.height)
        layout(width, height) {
            mine.place(mineX, 0)
            if (first != null && firstX + firstLabel.width + gap <= mineX) firstLabel.place(firstX, 0)
            if (last != null && lastX >= mineX + mine.width + gap) lastLabel.place(lastX, 0)
        }
    }
}

@Composable
private fun BusMarker(
    color: Color,
    border: Color,
    fraction: Float,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        if (fraction > 0f) Spacer(Modifier.weight(fraction))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(border)
                .padding(2.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.DirectionsBus,
                contentDescription = null,
                tint = MaterialTheme.melon.fixed.onHero,
                modifier = Modifier.size(13.dp),
            )
        }
        if (fraction < 1f) Spacer(Modifier.weight(1f - fraction))
    }
}

// ───────── Containers ─────────

// The card: hairline border, 20dp corners, on the card surface.
@Composable
internal fun Modifier.commuteCard(): Modifier {
    val shape = RoundedCornerShape(20.dp)
    return this
        .clip(shape)
        .background(MaterialTheme.melon.surface.card)
        .border(1.dp, MaterialTheme.melon.surface.line, shape)
}

@Composable
internal fun CommuteSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 9.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.42).sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(vertical = 4.dp),
            )
        }
    }
}

// A short stop name for the route line's end labels.
@Composable
internal fun transitShortName(
    stop: TransitStop,
    route: TransitRoute,
): String {
    if (stop.id == route.campusStopId) return stringResource(R.string.commute_campus_short)
    val terminal = if (route.direction ==
        TransitDirection.ToCampus
    ) {
        route.stops.firstOrNull()
    } else {
        route.stops.lastOrNull()
    }
    return if (stop.id == terminal?.id) stringResource(R.string.commute_terminal) else stop.name
}

@Composable
internal fun CommuteBulletIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .commuteCard(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size * 0.44f),
        )
    }
}

@Preview
@Composable
private fun CommuteComponentsPreview() {
    val now = Clock.System.now()
    val arrival = CommuteFixtures.board(now).arrivals[0]
    MelonTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransitLineBadge(number = "003")
                TransitLineBadge(number = "087", size = TransitBadgeSize.Large)
                TransitLineBadge(number = "088", size = TransitBadgeSize.Small, ghost = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransitChip(kind = TransitChipKind.Live)
                TransitChip(kind = TransitChipKind.Waiting)
                TransitChip(kind = TransitChipKind.Flaky, small = true)
            }
            TransitEstimateText(estimate = CommuteFormat.estimate(arrival, now))
            TransitFreshness(fixAt = now - 8.seconds, now = now)
            Column(
                modifier = Modifier
                    .commuteCard()
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp),
            ) {
                TransitRouteProgress(
                    stopNames = listOf("Terminal", "a", "b", "c", "d", "e", "UEFS"),
                    busPosition = 2.4,
                    mineIndex = 4,
                    color = MaterialTheme.melon.transit.line087,
                )
            }
        }
    }
}
