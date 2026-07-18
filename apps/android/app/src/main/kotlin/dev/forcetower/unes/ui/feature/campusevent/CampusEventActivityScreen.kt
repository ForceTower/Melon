package dev.forcetower.unes.ui.feature.campusevent

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEvent
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventActivity
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.melon

// One schedule activity: category hero, description, signup notice, hosts
// and the venue link. Mirrors iOS `CampusEventActivityScreen`.
@Composable
internal fun CampusEventActivityScreen(
    activityId: String,
    onBack: () -> Unit,
    onOpenVenues: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CampusEventViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val event = state.event ?: return
    val activity = event.activities.firstOrNull { it.id == activityId } ?: return

    CampusEventDetailScaffold(
        title = stringResource(activity.category.label),
        tone = activity.category.tone(),
        onBack = onBack,
        modifier = modifier,
        bottomInset = bottomInset,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Hero(
                event = event,
                activity = activity,
                modifier = Modifier.scaleInOnAppear(delayMs = 20, fromScale = 0.97f),
            )

            if (activity.details != null) {
                Text(
                    text = activity.details.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.5.sp,
                        lineHeight = 24.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fadeUpOnAppear(delayMs = 100)
                        .fillMaxWidth()
                        .campusEventCard()
                        .padding(16.dp),
                )
            }

            if (activity.requiresSignup) {
                SignupNotice(modifier = Modifier.fadeUpOnAppear(delayMs = 140))
            }

            if (activity.speakerNames.isNotEmpty()) {
                Speakers(
                    event = event,
                    activity = activity,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 180),
                )
            }

            VenueLink(
                event = event,
                activity = activity,
                onOpenVenues = {
                    vm.trackHubOpen("venues")
                    onOpenVenues()
                },
                modifier = Modifier.fadeUpOnAppear(delayMs = 220),
            )
        }
    }
}

@Composable
private fun Hero(
    event: CampusEvent,
    activity: CampusEventActivity,
    modifier: Modifier = Modifier,
) {
    val onHero = MaterialTheme.melon.fixed.onHero
    val veil = MaterialTheme.melon.fixed.heroVeil
    val zone = CampusEventFormat.zoneId(event.timeZoneIdentifier)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.melon.fixed.heroNight),
    ) {
        Mesh(variant = activity.category.mesh, modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to veil.copy(alpha = 0.14f),
                        1f to veil.copy(alpha = 0.60f),
                    ),
                ),
        )

        Column(modifier = Modifier.padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(onHero.copy(alpha = 0.18f))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = activity.category.icon,
                        contentDescription = null,
                        tint = onHero,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = stringResource(activity.category.label),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = onHero,
                    )
                }
                // Audience chip on-dark: white capsule instead of the tinted one.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(onHero.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(onHero),
                    )
                    Text(
                        text = stringResource(activity.audience.label),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = onHero,
                    )
                }
            }

            Text(
                text = activity.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 25.sp,
                    lineHeight = 30.sp,
                ),
                color = onHero,
                modifier = Modifier.padding(top = 14.dp),
            )

            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(onHero.copy(alpha = 0.16f)),
            )
            Spacer(Modifier.height(15.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                HeroMeta(
                    icon = Icons.Outlined.Schedule,
                    label = stringResource(R.string.campus_event_detail_time),
                    value = "${CampusEventFormat.weekdayShort(activity.startsAt, zone)} · " +
                        CampusEventFormat.timeRange(activity.startsAt, activity.endsAt, zone),
                    modifier = Modifier.weight(1f),
                )
                HeroMeta(
                    icon = Icons.Outlined.LocationOn,
                    label = stringResource(R.string.campus_event_detail_venue),
                    value = activity.venueName,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeroMeta(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = onHero.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = onHero.copy(alpha = 0.55f),
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.5.sp),
            color = onHero,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SignupNotice(modifier: Modifier = Modifier) {
    val amber = MaterialTheme.melon.palette.amber
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(amber.copy(alpha = 0.10f))
            .border(1.dp, amber.copy(alpha = 0.20f), shape)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(amber.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ConfirmationNumber,
                contentDescription = null,
                tint = MaterialTheme.melon.status.warn,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = stringResource(R.string.campus_event_detail_signup),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Speakers(
    event: CampusEvent,
    activity: CampusEventActivity,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        CampusEventSectionHeader(
            title = stringResource(
                if (activity.speakerNames.size > 1) {
                    R.string.campus_event_detail_speakers_many
                } else {
                    R.string.campus_event_detail_speakers_one
                },
            ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .campusEventCard(),
        ) {
            activity.speakerNames.forEachIndexed { index, name ->
                val speaker = event.speakerFor(activity, index)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    CampusEventAvatar(name = name)
                    Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = speaker?.role
                                ?: stringResource(R.string.campus_event_detail_speaker_fallback),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                }
                if (index < activity.speakerNames.lastIndex) {
                    CampusEventRowDivider(startIndent = 69.dp)
                }
            }
        }
    }
}

@Composable
private fun VenueLink(
    event: CampusEvent,
    activity: CampusEventActivity,
    onOpenVenues: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val venue = event.venueFor(activity)
    val tone = venue?.let { campusEventTone(it.name) } ?: MaterialTheme.melon.palette.coral
    Column(modifier = modifier) {
        CampusEventSectionHeader(title = stringResource(R.string.campus_event_detail_where))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .campusEventCard()
                .clickable(onClick = onOpenVenues)
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(tone.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.venueName,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = venue?.hint ?: stringResource(R.string.campus_event_detail_where_hint),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
