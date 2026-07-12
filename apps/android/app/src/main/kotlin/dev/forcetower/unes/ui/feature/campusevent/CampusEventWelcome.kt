package dev.forcetower.unes.ui.feature.campusevent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEvent
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventPhase
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeInOnAppear
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import kotlin.time.Instant

// Fullscreen always-dark reveal shown the first time an event edition is
// opened: staggered identity lockup over the warm mesh, then a phase-aware
// middle block (countdown / live day / thanks) and the enter button.
// Mirrors iOS `CampusEventWelcomeView`.
@Composable
internal fun CampusEventWelcome(
    event: CampusEvent,
    now: Instant,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val night = MaterialTheme.melon.fixed.night
    val veil = MaterialTheme.melon.fixed.nightVeil
    val onHero = MaterialTheme.melon.fixed.onHero
    val phase = event.phase(now)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(night),
    ) {
        Mesh(variant = MeshVariant.Warm, modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        0f to veil.copy(alpha = 0f),
                        1f to veil.copy(alpha = 0.72f),
                        radius = 1600f,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (event.institution != null) {
                Text(
                    text = event.institution.orEmpty().uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 13.sp,
                        letterSpacing = 2.08.sp,
                    ),
                    color = onHero.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 150),
                )
                Spacer(Modifier.height(20.dp))
            }

            Text(
                text = stringResource(R.string.campus_event_welcome_greeting),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                color = onHero.copy(alpha = 0.86f),
                modifier = Modifier.fadeUpOnAppear(delayMs = 350),
            )
            Spacer(Modifier.height(10.dp))

            if (event.edition != null) {
                Text(
                    text = event.edition.orEmpty(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 16.sp,
                        letterSpacing = 5.44.sp,
                    ),
                    color = onHero.copy(alpha = 0.62f),
                    modifier = Modifier.fadeUpOnAppear(delayMs = 500),
                )
            }
            Text(
                text = event.name,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 68.sp,
                    lineHeight = 64.sp,
                    letterSpacing = (-3.4).sp,
                ),
                color = onHero,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .scaleInOnAppear(delayMs = 660, fromScale = 0.94f),
            )

            if (event.tagline != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = event.tagline.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.5.sp),
                    color = onHero.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .fadeUpOnAppear(delayMs = 860),
                )
            }

            Spacer(Modifier.height(30.dp))
            MidBlock(event, phase, now, modifier = Modifier.fadeUpOnAppear(delayMs = 1060))

            Spacer(Modifier.height(30.dp))
            EnterButton(
                phase = phase,
                onEnter = onEnter,
                modifier = Modifier.fadeUpOnAppear(delayMs = 1280),
            )
        }

        SkipButton(
            onEnter = onEnter,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 12.dp, end = 18.dp)
                .fadeInOnAppear(delayMs = 1400),
        )
    }
}

@Composable
private fun SkipButton(onEnter: () -> Unit, modifier: Modifier = Modifier) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Text(
        text = stringResource(R.string.campus_event_welcome_skip),
        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
        color = onHero,
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(onHero.copy(alpha = 0.14f))
            .clickable(onClick = onEnter)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun MidBlock(
    event: CampusEvent,
    phase: CampusEventPhase,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    val onHero = MaterialTheme.melon.fixed.onHero
    val context = LocalContext.current
    val zone = CampusEventFormat.zoneId(event.timeZoneIdentifier)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(
                when (phase) {
                    CampusEventPhase.Upcoming -> R.string.campus_event_welcome_starts_in
                    CampusEventPhase.Live -> R.string.campus_event_welcome_happening
                    CampusEventPhase.Ended -> R.string.campus_event_phase_ended
                },
            ).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
            color = onHero.copy(alpha = 0.6f),
        )

        GlassPanel {
            when (phase) {
                CampusEventPhase.Upcoming ->
                    CampusEventCountdownRow(target = event.startsAt, now = now)
                CampusEventPhase.Live -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    modifier = Modifier.padding(horizontal = 6.dp),
                ) {
                    CampusEventLiveDot(size = 9.dp)
                    Text(
                        text = stringResource(
                            R.string.campus_event_day_of,
                            event.dayNumber(now),
                            event.dayCount,
                        ),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = onHero,
                    )
                }
                CampusEventPhase.Ended -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    modifier = Modifier.padding(horizontal = 2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.melon.fixed.okOnDark,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(R.string.campus_event_welcome_thanks),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = onHero.copy(alpha = 0.9f),
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = onHero.copy(alpha = 0.7f),
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = CampusEventFormat.dateRange(
                    context,
                    event.startsAt,
                    event.endsAt,
                    zone,
                    withYear = true,
                ),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                color = onHero.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
private fun GlassPanel(content: @Composable () -> Unit) {
    val onHero = MaterialTheme.melon.fixed.onHero
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(onHero.copy(alpha = 0.08f))
            .border(1.dp, onHero.copy(alpha = 0.14f), shape)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun EnterButton(
    phase: CampusEventPhase,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.melon.fixed.surfaceLight)
            .clickable(onClick = onEnter)
            .padding(horizontal = 30.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        val content = MaterialTheme.melon.fixed.onSurfaceLight
        Text(
            text = stringResource(
                when (phase) {
                    CampusEventPhase.Upcoming -> R.string.campus_event_welcome_enter_upcoming
                    CampusEventPhase.Live -> R.string.campus_event_welcome_enter_live
                    CampusEventPhase.Ended -> R.string.campus_event_welcome_enter_ended
                },
            ),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
            color = content,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(18.dp),
        )
    }
}
