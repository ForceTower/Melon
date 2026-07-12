package dev.forcetower.unes.ui.feature.campusevent

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEvent
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventPhase
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.melon
import kotlin.time.Instant

// The featured event card on Home: always-dark warm mesh with the event
// identity, a phase-aware right block (countdown / day counter / ended) and
// a date + call-to-action footer. The whole card is the tap target. Mirrors
// iOS `CampusEventHomeCard` and the dc `SiecompScreen` home card. Carries its
// own per-second clock (like the iOS `TimelineView`) so the phase flips the
// moment the event starts, without ticking the rest of Hoje.
@Composable
internal fun CampusEventHomeCard(
    event: CampusEvent,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val veil = MaterialTheme.melon.fixed.heroVeil
    val now = rememberCampusEventNow()
    val phase = event.phase(now)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.melon.fixed.heroNight)
            .clickable(onClick = onOpen),
    ) {
        Mesh(variant = MeshVariant.Warm, modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to veil.copy(alpha = 0.08f),
                        1f to veil.copy(alpha = 0.66f),
                    ),
                ),
        )

        Column(modifier = Modifier.padding(start = 19.dp, top = 17.dp, end = 19.dp, bottom = 18.dp)) {
            EyebrowRow(phase)
            Spacer(Modifier.height(16.dp))
            IdentityRow(event, phase, now)
            Spacer(Modifier.height(18.dp))
            Footer(event, phase)
        }
    }
}

@Composable
private fun EyebrowRow(phase: CampusEventPhase) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (phase != CampusEventPhase.Ended) {
            CampusEventLiveDot()
        }
        Text(
            text = stringResource(
                when (phase) {
                    CampusEventPhase.Upcoming -> R.string.campus_event_card_upcoming
                    CampusEventPhase.Live -> R.string.campus_event_phase_live
                    CampusEventPhase.Ended -> R.string.campus_event_phase_ended
                },
            ).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
            color = onHero.copy(alpha = 0.9f),
        )
    }
}

@Composable
private fun IdentityRow(event: CampusEvent, phase: CampusEventPhase, now: Instant) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (event.edition != null) {
                Text(
                    text = event.edition.orEmpty(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 13.sp,
                        letterSpacing = 1.82.sp,
                    ),
                    color = onHero.copy(alpha = 0.72f),
                )
            }
            Text(
                text = event.name,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 40.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = onHero,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (event.tagline != null) {
                Text(
                    text = event.tagline.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                    color = onHero.copy(alpha = 0.82f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        PhaseBlock(event, phase, now)
    }
}

@Composable
private fun PhaseBlock(event: CampusEvent, phase: CampusEventPhase, now: Instant) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (phase) {
            CampusEventPhase.Upcoming -> {
                PhaseLabel(stringResource(R.string.campus_event_card_starts_in))
                val countdown = CampusEventFormat.countdown(event.startsAt, now)
                BigValue(
                    stringResource(
                        R.string.campus_event_card_countdown_format,
                        countdown.days,
                        CampusEventFormat.padded(countdown.hours),
                    ),
                )
            }
            CampusEventPhase.Live -> {
                PhaseLabel(stringResource(R.string.campus_event_card_day))
                Row(verticalAlignment = Alignment.Bottom) {
                    BigValue(event.dayNumber(now).toString())
                    Text(
                        text = stringResource(R.string.campus_event_card_day_of_count, event.dayCount),
                        style = MaterialTheme.typography.titleSmall,
                        color = onHero.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }
            CampusEventPhase.Ended -> {
                Text(
                    text = stringResource(R.string.campus_event_card_ended),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = onHero,
                )
            }
        }
    }
}

@Composable
private fun PhaseLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
        color = MaterialTheme.melon.fixed.onHero.copy(alpha = 0.55f),
    )
}

@Composable
private fun BigValue(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp, lineHeight = 30.sp),
        color = MaterialTheme.melon.fixed.onHero,
    )
}

@Composable
private fun Footer(event: CampusEvent, phase: CampusEventPhase) {
    val onHero = MaterialTheme.melon.fixed.onHero
    val context = LocalContext.current
    val zone = CampusEventFormat.zoneId(event.timeZoneIdentifier)
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(onHero.copy(alpha = 0.15f)),
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = onHero.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = CampusEventFormat.dateRange(context, event.startsAt, event.endsAt, zone),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                color = onHero.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(
                    when (phase) {
                        CampusEventPhase.Upcoming -> R.string.campus_event_card_open
                        CampusEventPhase.Live -> R.string.campus_event_card_follow
                        CampusEventPhase.Ended -> R.string.campus_event_card_relive
                    },
                ),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                color = onHero,
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(onHero.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = onHero,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
