package dev.forcetower.unes.ui.feature.overview.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.overview.OverviewFixtures
import dev.forcetower.unes.ui.feature.overview.OverviewHeroClass
import dev.forcetower.unes.ui.feature.overview.OverviewHeroState
import dev.forcetower.unes.ui.feature.overview.OverviewTomorrowUi
import kotlin.math.roundToInt

// Mesh hero card — the design's contained "Próxima aula" card and its two
// alternate states: live class (3a) and day concluded (3b).
@Composable
internal fun HeroCard(
    state: OverviewHeroState,
    firstName: String,
    tomorrowEyebrow: String,
    isEvening: Boolean,
    onOpenClassDetails: (OverviewHeroClass) -> Unit,
    modifier: Modifier = Modifier,
) {
    val variant = when (state) {
        is OverviewHeroState.DayDone -> MeshVariant.Dusk
        else -> MeshVariant.Hero
    }
    val veil = MaterialTheme.melon.fixed.heroVeil

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.melon.fixed.heroNight),
    ) {
        // matchParentSize (not fillMaxSize): inside a scroll the card's height
        // comes from the content Column; the mesh + veil stretch to match it.
        Mesh(variant = variant, modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to veil.copy(alpha = 0.28f),
                        0.45f to veil.copy(alpha = 0.10f),
                        1f to veil.copy(alpha = 0.32f),
                    ),
                ),
        )

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
            when (state) {
                is OverviewHeroState.Upcoming -> UpcomingContent(state, onOpenClassDetails)
                is OverviewHeroState.Live -> LiveContent(state)
                is OverviewHeroState.DayDone -> DayDoneContent(
                    state = state,
                    firstName = firstName,
                    tomorrowEyebrow = tomorrowEyebrow,
                    isEvening = isEvening,
                )
            }
        }
    }
}

@Composable
private fun UpcomingContent(
    state: OverviewHeroState.Upcoming,
    onOpenClassDetails: (OverviewHeroClass) -> Unit,
) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.overview_hero_next_label).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = onHero.copy(alpha = 0.75f),
            )
            HeroChip(icon = Icons.Outlined.Schedule, label = startsInLabel(state.startsInMinutes))
        }
        Spacer(Modifier.height(16.dp))
        HeroTitle(state.klass.title)
        Spacer(Modifier.height(16.dp))
        HeroInfoRows(state.klass)
        Spacer(Modifier.height(18.dp))
        HeroCtaButton(
            enabled = state.klass.offerId != null,
            onClick = { onOpenClassDetails(state.klass) },
        )
    }
}

@Composable
private fun LiveContent(state: OverviewHeroState.Live) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LivePulseDot()
                Spacer(Modifier.width(7.dp))
                Text(
                    text = stringResource(R.string.overview_hero_now_label).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.melon.fixed.liveText,
                )
            }
            HeroChip(icon = Icons.Outlined.Timelapse, label = endsInLabel(state.endsInMinutes))
        }
        Spacer(Modifier.height(16.dp))
        HeroTitle(state.klass.title)
        Spacer(Modifier.height(16.dp))
        HeroInfoRows(state.klass)
        Spacer(Modifier.height(18.dp))

        val percent = (state.progress.coerceIn(0f, 1f) * 100).roundToInt()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.overview_hero_progress_label),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = onHero.copy(alpha = 0.82f),
            )
            Text(
                text = stringResource(R.string.overview_hero_progress_value, percent),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = onHero,
            )
        }
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(onHero.copy(alpha = 0.20f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = state.progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(onHero),
            )
        }
    }
}

@Composable
private fun DayDoneContent(
    state: OverviewHeroState.DayDone,
    firstName: String,
    tomorrowEyebrow: String,
    isEvening: Boolean,
) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.TaskAlt,
                contentDescription = null,
                tint = MaterialTheme.melon.fixed.liveText,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.overview_hero_done_label).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.melon.fixed.liveText,
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = pluralStringResource(
                R.plurals.overview_hero_done_title,
                state.classCount,
                state.classCount,
            ),
            style = MaterialTheme.typography.headlineSmall.copy(shadow = heroTextShadow()),
            color = onHero,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                if (isEvening) R.string.overview_hero_done_rest_evening
                else R.string.overview_hero_done_rest_day,
                firstName,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = onHero.copy(alpha = 0.75f),
        )

        if (state.tomorrow != null) {
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(onHero.copy(alpha = 0.16f)),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(
                    R.string.overview_hero_tomorrow_eyebrow,
                    tomorrowEyebrow,
                ).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = onHero.copy(alpha = 0.60f),
            )
            Spacer(Modifier.height(12.dp))
            TomorrowRow(state.tomorrow)
        }
    }
}

@Composable
private fun TomorrowRow(tomorrow: OverviewTomorrowUi) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(onHero.copy(alpha = 0.10f))
            .border(1.dp, onHero.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val hour = tomorrow.startTime.take(5).substringBefore(':')
        val minute = tomorrow.startTime.take(5).substringAfter(':', missingDelimiterValue = "")
        Column(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(onHero.copy(alpha = 0.14f)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = hour,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 16.sp,
                ),
                color = onHero,
            )
            if (minute.isNotEmpty()) {
                Text(
                    text = ":$minute",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = onHero.copy(alpha = 0.7f),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tomorrow.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = onHero,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            val extras = if (tomorrow.extraCount > 0) {
                pluralStringResource(
                    R.plurals.overview_hero_tomorrow_extra,
                    tomorrow.extraCount,
                    tomorrow.extraCount,
                )
            } else null
            Text(
                text = listOfNotNull(tomorrow.room, extras).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = onHero.copy(alpha = 0.72f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HeroTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge.copy(shadow = heroTextShadow()),
        color = MaterialTheme.melon.fixed.onHero,
    )
}

@Composable
private fun heroTextShadow(): Shadow = Shadow(
    color = MaterialTheme.melon.fixed.heroVeil.copy(alpha = 0.35f),
    offset = Offset(0f, 2f),
    blurRadius = 24f,
)

@Composable
private fun HeroInfoRows(klass: OverviewHeroClass) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HeroInfoRow(icon = Icons.Outlined.Schedule, label = klass.timeRange)
        klass.room?.let { HeroInfoRow(icon = Icons.Outlined.LocationOn, label = it) }
        klass.prof?.let { HeroInfoRow(icon = Icons.Outlined.Person, label = it) }
    }
}

@Composable
private fun HeroInfoRow(icon: ImageVector, label: String) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onHero.copy(alpha = 0.70f),
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = onHero.copy(alpha = 0.92f),
            maxLines = 1,
        )
    }
}

@Composable
private fun HeroChip(icon: ImageVector, label: String) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(onHero.copy(alpha = 0.18f))
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onHero,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = onHero,
        )
    }
}

@Composable
private fun HeroCtaButton(enabled: Boolean, onClick: () -> Unit) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(onHero.copy(alpha = 0.12f))
            .border(1.dp, onHero.copy(alpha = 0.30f), RoundedCornerShape(100.dp))
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.overview_hero_details_action),
            style = MaterialTheme.typography.labelLarge,
            color = onHero,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = onHero,
            modifier = Modifier.size(18.dp),
        )
    }
}

// The design's `livePulse` keyframes: a solid green dot with an expanding,
// fading halo ring on a 1.8s loop.
@Composable
private fun LivePulseDot() {
    val live = MaterialTheme.melon.fixed.live
    val transition = rememberInfiniteTransition(label = "live-pulse")
    val halo by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "halo",
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .drawBehind {
                val maxExtra = 8.dp.toPx()
                drawCircle(
                    color = live.copy(alpha = 0.6f * (1f - halo)),
                    radius = size.minDimension / 2f + maxExtra * halo,
                )
            }
            .clip(CircleShape)
            .background(live),
    )
}

// ───────── formatting ─────────

@Composable
private fun startsInLabel(startsInMinutes: Int): String {
    val minutes = startsInMinutes.coerceAtLeast(0)
    return when {
        minutes < 60 -> stringResource(R.string.overview_hero_in_minutes, minutes)
        minutes < 24 * 60 -> stringResource(
            R.string.overview_hero_in_hours,
            (minutes + 30) / 60,
        )
        else -> stringResource(R.string.overview_hero_in_days, minutes / (24 * 60))
    }
}

@Composable
private fun endsInLabel(endsInMinutes: Int): String {
    val minutes = endsInMinutes.coerceAtLeast(0)
    return if (minutes < 60) {
        stringResource(R.string.overview_hero_ends_minutes, minutes)
    } else {
        stringResource(R.string.overview_hero_ends_hour_min, minutes / 60, minutes % 60)
    }
}

@Preview
@Composable
private fun HeroCardUpcomingPreview() {
    MelonTheme {
        HeroCard(
            state = OverviewFixtures.heroUpcoming,
            firstName = "Marina",
            tomorrowEyebrow = OverviewFixtures.TOMORROW_EYEBROW,
            isEvening = true,
            onOpenClassDetails = {},
        )
    }
}

@Preview
@Composable
private fun HeroCardLivePreview() {
    MelonTheme {
        HeroCard(
            state = OverviewFixtures.heroLive,
            firstName = "Marina",
            tomorrowEyebrow = OverviewFixtures.TOMORROW_EYEBROW,
            isEvening = true,
            onOpenClassDetails = {},
        )
    }
}

@Preview
@Composable
private fun HeroCardDayDonePreview() {
    MelonTheme {
        HeroCard(
            state = OverviewFixtures.heroDayDone,
            firstName = "Marina",
            tomorrowEyebrow = OverviewFixtures.TOMORROW_EYEBROW,
            isEvening = true,
            onOpenClassDetails = {},
        )
    }
}
