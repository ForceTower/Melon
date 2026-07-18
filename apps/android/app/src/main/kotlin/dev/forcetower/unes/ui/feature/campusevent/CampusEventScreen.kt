package dev.forcetower.unes.ui.feature.campusevent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEvent
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventActivity
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventActivityState
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventAudience
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventCategory
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventDay
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventPhase
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.MelonSegmentedRow
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeInOnAppear
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import java.time.ZoneId
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

// The campus-event hub (dc `SiecompScreen`, iOS `CampusEventView`): header,
// phase-aware mesh hero, quick links into the detail screens and the
// day-by-day schedule with an audience filter. The fullscreen welcome reveal
// overlays everything the first time an event edition is opened.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CampusEventScreen(
    onBack: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onOpenSpeakers: () -> Unit,
    onOpenWorkshops: () -> Unit,
    onOpenVenues: () -> Unit,
    onOpenOrganizations: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CampusEventViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val event = state.event
    val now = rememberCampusEventNow()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (event != null) {
            AmbientWash()
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { vm.onIntent(CampusEventIntent.RefreshPulled) },
            ) {
                Hub(
                    event = event,
                    now = now,
                    filter = state.filter,
                    selectedDay = state.selectedDay,
                    bottomInset = bottomInset,
                    onBack = onBack,
                    onDayTapped = { vm.onIntent(CampusEventIntent.DayTapped(it)) },
                    onFilterChanged = { vm.onIntent(CampusEventIntent.FilterChanged(it)) },
                    onOpenActivity = onOpenActivity,
                    onOpenSpeakers = onOpenSpeakers,
                    onOpenWorkshops = onOpenWorkshops,
                    onOpenVenues = onOpenVenues,
                    onOpenOrganizations = onOpenOrganizations,
                )
            }

            AnimatedVisibility(
                visible = state.isShowingWelcome,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                CampusEventWelcome(
                    event = event,
                    now = now,
                    onEnter = { vm.onIntent(CampusEventIntent.WelcomeContinueTapped) },
                )
            }
        }
    }
}

// Faint warm mesh washing down from behind the large title.
@Composable
private fun AmbientWash() {
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        Mesh(
            variant = MeshVariant.Warm,
            intensity = 0.5f,
            modifier = Modifier
                .matchParentSize()
                .alpha(0.28f),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to background.copy(alpha = 0.55f),
                        1f to background,
                    ),
                ),
        )
    }
}

@Composable
private fun Hub(
    event: CampusEvent,
    now: Instant,
    filter: CampusEventAudience,
    selectedDay: LocalDate?,
    bottomInset: Dp,
    onBack: () -> Unit,
    onDayTapped: (LocalDate) -> Unit,
    onFilterChanged: (CampusEventAudience) -> Unit,
    onOpenActivity: (String) -> Unit,
    onOpenSpeakers: () -> Unit,
    onOpenWorkshops: () -> Unit,
    onOpenVenues: () -> Unit,
    onOpenOrganizations: () -> Unit,
) {
    val zone = CampusEventFormat.zoneId(event.timeZoneIdentifier)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomInset + 32.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .fadeInOnAppear(delayMs = 20),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.campus_event_back_label),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Header(
            event = event,
            zone = zone,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fadeInOnAppear(delayMs = 40),
        )

        CampusEventHubHero(
            event = event,
            filter = filter,
            now = now,
            zone = zone,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 14.dp)
                .scaleInOnAppear(delayMs = 100, fromScale = 0.97f),
        )

        QuickLinks(
            event = event,
            onOpenSpeakers = onOpenSpeakers,
            onOpenWorkshops = onOpenWorkshops,
            onOpenVenues = onOpenVenues,
            onOpenOrganizations = onOpenOrganizations,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 20.dp)
                .fadeUpOnAppear(delayMs = 180),
        )

        Schedule(
            event = event,
            now = now,
            zone = zone,
            filter = filter,
            selectedDay = selectedDay,
            onDayTapped = onDayTapped,
            onFilterChanged = onFilterChanged,
            onOpenActivity = onOpenActivity,
            modifier = Modifier
                .padding(top = 26.dp)
                .fadeUpOnAppear(delayMs = 260),
        )

        if (event.credit != null) {
            Text(
                text = event.credit.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 22.dp)
                    .fadeUpOnAppear(delayMs = 320),
            )
        }
    }
}

@Composable
private fun Header(event: CampusEvent, zone: ZoneId, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val range = CampusEventFormat.dateRange(context, event.startsAt, event.endsAt, zone)
    Column(modifier = modifier) {
        if (event.edition != null) {
            Text(
                text = event.edition.orEmpty().uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Text(
            text = range.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = event.name,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.64).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (event.tagline != null) {
            Text(
                text = event.tagline.orEmpty(),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 7.dp),
            )
        }
    }
}

// MARK: Hero

// Phase-aware dark hero: countdown before the event, day progress and the
// live/next activity while it runs, thanks + stats after.
@Composable
private fun CampusEventHubHero(
    event: CampusEvent,
    filter: CampusEventAudience,
    now: Instant,
    zone: ZoneId,
    modifier: Modifier = Modifier,
) {
    val onHero = MaterialTheme.melon.fixed.onHero
    val veil = MaterialTheme.melon.fixed.heroVeil
    val phase = event.phase(now)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.melon.fixed.heroNight),
    ) {
        Mesh(
            variant = if (phase == CampusEventPhase.Ended) MeshVariant.Fresh else MeshVariant.Warm,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to veil.copy(alpha = 0.14f),
                        1f to veil.copy(alpha = 0.64f),
                    ),
                ),
        )

        Column(modifier = Modifier.padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (phase != CampusEventPhase.Ended) {
                    CampusEventLiveDot()
                }
                val liveCount = event.liveActivityCount(filter, now)
                Text(
                    text = when {
                        phase == CampusEventPhase.Upcoming ->
                            stringResource(R.string.campus_event_phase_upcoming)
                        phase == CampusEventPhase.Ended ->
                            stringResource(R.string.campus_event_phase_ended)
                        liveCount > 0 -> pluralStringResource(
                            R.plurals.campus_event_phase_live_count,
                            liveCount,
                            liveCount,
                        )
                        else -> stringResource(R.string.campus_event_phase_live)
                    }.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    color = onHero.copy(alpha = 0.9f),
                )
            }

            Spacer(Modifier.height(16.dp))
            when (phase) {
                CampusEventPhase.Upcoming -> Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CampusEventCountdownRow(target = event.startsAt, now = now)
                }
                CampusEventPhase.Live -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.campus_event_day_of,
                                event.dayNumber(now),
                                event.dayCount,
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 13.sp,
                                letterSpacing = 0.sp,
                            ),
                            color = onHero,
                        )
                        Text(
                            text = CampusEventFormat.fullDate(now, zone),
                            style = MaterialTheme.typography.bodySmall,
                            color = onHero.copy(alpha = 0.6f),
                        )
                    }
                    CampusEventDayProgress(
                        dayCount = event.dayCount,
                        currentIndex = event.dayNumber(now) - 1,
                    )
                }
                CampusEventPhase.Ended -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.melon.fixed.live.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.melon.fixed.okOnDark,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.campus_event_hub_ended_title),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = onHero,
                        )
                        Text(
                            text = stringResource(R.string.campus_event_hub_ended_body),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                            color = onHero.copy(alpha = 0.78f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(onHero.copy(alpha = 0.14f)),
            )
            Spacer(Modifier.height(14.dp))

            when (phase) {
                CampusEventPhase.Upcoming -> event.opener?.let { opener ->
                    CampusEventHeroActivityBlock(
                        activity = opener,
                        tag = stringResource(R.string.campus_event_hub_opening),
                        zone = zone,
                    )
                }
                CampusEventPhase.Live -> event.liveOrNextActivity(filter, now)?.let { (activity, isLive) ->
                    CampusEventHeroActivityBlock(
                        activity = activity,
                        tag = stringResource(
                            if (isLive) R.string.campus_event_hub_now else R.string.campus_event_hub_next,
                        ),
                        tagTone = if (isLive) {
                            MaterialTheme.melon.fixed.okOnDark
                        } else {
                            onHero.copy(alpha = 0.55f)
                        },
                        zone = zone,
                    )
                }
                CampusEventPhase.Ended -> Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    CampusEventHeroStat(
                        value = event.activities.size,
                        label = stringResource(R.string.campus_event_hub_stat_activities),
                    )
                    if (event.speakers.isNotEmpty()) {
                        CampusEventHeroStat(
                            value = event.speakers.size,
                            label = stringResource(R.string.campus_event_hub_stat_speakers),
                        )
                    }
                    CampusEventHeroStat(
                        value = event.dayCount,
                        label = stringResource(R.string.campus_event_hub_stat_days),
                    )
                }
            }
        }
    }
}

// MARK: Quick links

@Composable
private fun QuickLinks(
    event: CampusEvent,
    onOpenSpeakers: () -> Unit,
    onOpenWorkshops: () -> Unit,
    onOpenVenues: () -> Unit,
    onOpenOrganizations: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.melon.palette
    val tiles = buildList {
        if (event.speakers.isNotEmpty()) {
            add(
                QuickTileData(
                    icon = Icons.Outlined.Mic,
                    tone = palette.violet,
                    label = stringResource(R.string.campus_event_hub_quick_speakers),
                    note = pluralStringResource(
                        R.plurals.campus_event_hub_quick_speakers_count,
                        event.speakers.size,
                        event.speakers.size,
                    ),
                    onTap = onOpenSpeakers,
                ),
            )
        }
        if (event.workshops.isNotEmpty()) {
            add(
                QuickTileData(
                    icon = CampusEventCategory.Workshop.icon,
                    tone = palette.teal,
                    label = stringResource(R.string.campus_event_hub_quick_workshops),
                    note = pluralStringResource(
                        R.plurals.campus_event_hub_quick_workshops_count,
                        event.workshops.size,
                        event.workshops.size,
                    ),
                    onTap = onOpenWorkshops,
                ),
            )
        }
        if (event.venues.isNotEmpty()) {
            add(
                QuickTileData(
                    icon = Icons.Outlined.Map,
                    tone = palette.amber,
                    label = stringResource(R.string.campus_event_hub_quick_venues),
                    note = pluralStringResource(
                        R.plurals.campus_event_hub_quick_venues_count,
                        event.venues.size,
                        event.venues.size,
                    ),
                    onTap = onOpenVenues,
                ),
            )
        }
        if (event.organizations.isNotEmpty()) {
            add(
                QuickTileData(
                    icon = Icons.Outlined.Groups,
                    tone = palette.magenta,
                    label = stringResource(R.string.campus_event_hub_quick_organizations),
                    note = pluralStringResource(
                        R.plurals.campus_event_hub_quick_organizations_count,
                        event.organizations.size,
                        event.organizations.size,
                    ),
                    onTap = onOpenOrganizations,
                ),
            )
        }
    }
    if (tiles.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { tile ->
                    QuickTile(tile, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class QuickTileData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tone: Color,
    val label: String,
    val note: String,
    val onTap: () -> Unit,
)

@Composable
private fun QuickTile(data: QuickTileData, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .campusEventCard()
            .clickable(onClick = data.onTap),
    ) {
        // Soft tinted glow bleeding off the top-right corner.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(74.dp)
                .offsetGlow(data.tone),
        )
        Column(modifier = Modifier.padding(15.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(data.tone.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = data.tone,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = data.label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = data.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

private fun Modifier.offsetGlow(tone: Color): Modifier = this
    .background(
        Brush.radialGradient(
            colors = listOf(tone.copy(alpha = 0.13f), Color.Transparent),
        ),
    )

// MARK: Schedule

@Composable
private fun Schedule(
    event: CampusEvent,
    now: Instant,
    zone: ZoneId,
    filter: CampusEventAudience,
    selectedDay: LocalDate?,
    onDayTapped: (LocalDate) -> Unit,
    onFilterChanged: (CampusEventAudience) -> Unit,
    onOpenActivity: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = event.days()
    val isLive = event.phase(now) == CampusEventPhase.Live
    val selected = selectedDay ?: days.firstOrNull()?.date

    // Time-column width: sized to the widest start/end label this event
    // renders so times never wrap — 12-hour locales need "12:30 PM", 24-hour
    // ones stay compact at "08:00". Measured once per event/zone.
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val startStyle = MaterialTheme.typography.titleSmall.copy(
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Bold,
    )
    val endStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp)
    val timeColumnWidth = remember(event, zone, textMeasurer, startStyle, endStyle, density) {
        val widest = event.activities
            .flatMap { activity ->
                listOfNotNull(
                    CampusEventFormat.time(activity.startsAt, zone) to startStyle,
                    activity.endsAt?.let { CampusEventFormat.time(it, zone) to endStyle },
                )
            }
            .maxOfOrNull { (text, style) -> textMeasurer.measure(text, style).size.width }
            ?: 0
        with(density) { widest.toDp() }.coerceAtLeast(46.dp)
    }

    Column(modifier = modifier) {
        CampusEventSectionHeader(
            title = stringResource(R.string.campus_event_hub_schedule),
            note = stringResource(R.string.campus_event_hub_schedule_note),
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            days.forEach { day ->
                DayTab(
                    day = day,
                    isSelected = day.date == selected,
                    isToday = isLive && event.isOnDay(now, day.date),
                    hasActivities = event.activityCount(day.date, filter) > 0,
                    onTap = { onDayTapped(day.date) },
                )
            }
        }

        if (event.hasAudienceSplit) {
            AudienceFilter(
                filter = filter,
                onFilterChanged = onFilterChanged,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
            )
        }

        if (selected != null) {
            val activities = event.activities(selected, filter)
            Text(
                text = "${CampusEventFormat.weekdayLong(selected)} · " +
                    pluralStringResource(
                        R.plurals.campus_event_hub_activity_count,
                        activities.size,
                        activities.size,
                    ),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
            )

            if (activities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .campusEventCard()
                        .padding(vertical = 40.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.campus_event_hub_empty_day),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .campusEventCard(),
                ) {
                    activities.forEachIndexed { index, activity ->
                        ActivityRow(
                            activity = activity,
                            state = activity.state(now),
                            zone = zone,
                            timeColumnWidth = timeColumnWidth,
                            isLast = index == activities.lastIndex,
                            onTap = { onOpenActivity(activity.id) },
                        )
                    }
                }
            }
        }
    }
}

// One pill of the day strip: weekday, day number and a today/dot marker.
@Composable
private fun DayTab(
    day: CampusEventDay,
    isSelected: Boolean,
    isToday: Boolean,
    hasActivities: Boolean,
    onTap: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val ink = MaterialTheme.colorScheme.onBackground
    val surface = MaterialTheme.colorScheme.background
    val accent = MaterialTheme.colorScheme.primary
    val background = if (isSelected) ink else MaterialTheme.melon.surface.card
    val border = when {
        isSelected -> Color.Transparent
        isToday -> accent
        else -> MaterialTheme.melon.surface.cardLine
    }

    Column(
        modifier = Modifier
            .width(60.dp)
            .clip(shape)
            .background(background)
            .then(
                if (border == Color.Transparent) {
                    Modifier
                } else {
                    Modifier.border(
                        width = if (isToday) 1.5.dp else 1.dp,
                        color = border,
                        shape = shape,
                    )
                },
            )
            .clickable(onClick = onTap)
            .padding(top = 10.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = CampusEventFormat.weekdayShort(day.date).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.44.sp),
            color = if (isSelected) surface.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = CampusEventFormat.dayNumber(day.date),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 22.sp),
            color = if (isSelected) surface else ink,
        )
        Box(
            modifier = Modifier.height(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isToday) {
                Text(
                    text = stringResource(R.string.campus_event_hub_today).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, lineHeight = 12.sp),
                    color = if (isSelected) surface else accent,
                    maxLines = 1,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            when {
                                isSelected -> surface.copy(alpha = 0.7f)
                                hasActivities -> accent
                                else -> Color.Transparent
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun AudienceFilter(
    filter: CampusEventAudience,
    onFilterChanged: (CampusEventAudience) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        CampusEventAudience.Everyone,
        CampusEventAudience.Freshmen,
        CampusEventAudience.Veterans,
    )
    val labels = options.associateWith { stringResource(it.label) }
    val tones = options.associateWith { it.tone() }
    MelonSegmentedRow(
        options = options,
        selected = filter,
        onSelect = onFilterChanged,
        label = { audience -> labels.getValue(audience) },
        dot = { audience, active ->
            tones.getValue(audience).copy(alpha = if (active) 1f else 0.5f)
        },
        modifier = modifier,
    )
}

// One schedule entry: time column, category rail, badges + title + venue.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivityRow(
    activity: CampusEventActivity,
    state: CampusEventActivityState,
    zone: ZoneId,
    timeColumnWidth: Dp,
    isLast: Boolean,
    onTap: () -> Unit,
) {
    val tone = activity.category.tone()
    val isNow = state == CampusEventActivityState.Live
    val isPast = state == CampusEventActivityState.Past
    val ok = MaterialTheme.melon.status.ok

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clickable(onClick = onTap)
                .background(if (isNow) tone.copy(alpha = 0.07f) else Color.Transparent)
                .alpha(if (isPast) 0.55f else 1f)
                .padding(start = 14.dp, top = 13.dp, end = 12.dp, bottom = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.width(timeColumnWidth).padding(top = 2.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = CampusEventFormat.time(activity.startsAt, zone),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                if (activity.endsAt != null) {
                    Text(
                        text = CampusEventFormat.time(activity.endsAt!!, zone),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                    )
                }
            }

            // Category rail — stretches the full row like the dc design's
            // `align-items: stretch` bar.
            Box(
                modifier = Modifier
                    .width(if (isNow) 4.dp else 3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(tone),
            )

            Column(modifier = Modifier.weight(1f)) {
                // Badges wrap like the design's `flex-wrap` row — three chips
                // (category + audience + AGORA) can overflow narrow rows.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    CampusEventCategoryPill(category = activity.category)
                    CampusEventAudienceChip(audience = activity.audience)
                    if (isNow) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(ok.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            CampusEventLiveDot(size = 6.dp, color = ok)
                            Text(
                                text = stringResource(R.string.campus_event_hub_now).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    letterSpacing = 0.sp,
                                ),
                                color = ok,
                            )
                        }
                    }
                    if (isPast) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(11.dp),
                            )
                            Text(
                                text = stringResource(R.string.campus_event_row_done),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    letterSpacing = 0.sp,
                                ),
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.5.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (isPast) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.padding(top = 7.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = activity.venueName,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.CenterVertically),
            )
        }
        if (!isLast) {
            // Row leading padding + time column + gap — keeps the divider
            // aligned with the badge column whatever the time width is.
            CampusEventRowDivider(startIndent = timeColumnWidth + 28.dp)
        }
    }
}
