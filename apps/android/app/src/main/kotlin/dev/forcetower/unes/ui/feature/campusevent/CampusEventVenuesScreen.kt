package dev.forcetower.unes.ui.feature.campusevent

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEvent
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventVenue
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import kotlin.math.roundToInt

// Where the event happens: a schematic campus map (when the venues carry
// coordinates) over the venue list. Pin selection is view-only state.
// Mirrors iOS `CampusEventVenuesScreen` and the dc map screen.
@Composable
internal fun CampusEventVenuesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CampusEventViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val event = state.event ?: return
    val venues = event.venues
    val mapped = venues.filter { it.mapX != null && it.mapY != null }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    CampusEventDetailScaffold(
        title = stringResource(R.string.campus_event_venues_title),
        tone = MaterialTheme.melon.palette.amber,
        onBack = onBack,
        modifier = modifier,
        bottomInset = bottomInset,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.campus_event_venues_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp, end = 4.dp, bottom = 16.dp)
                    .fadeUpOnAppear(delayMs = 20),
            )

            if (mapped.isNotEmpty()) {
                CampusMap(
                    venues = venues,
                    mapped = mapped,
                    selectedId = selectedId,
                    onPinTapped = { id -> selectedId = if (selectedId == id) null else id },
                    modifier = Modifier
                        .padding(bottom = 18.dp)
                        .scaleInOnAppear(delayMs = 40, fromScale = 0.97f),
                )
            }

            CampusEventSectionHeader(
                title = stringResource(R.string.campus_event_venues_spaces),
                modifier = Modifier.fadeUpOnAppear(delayMs = 100),
            )
            VenueList(
                event = event,
                venues = venues,
                onTap = { selectedId = it },
                modifier = Modifier.fadeUpOnAppear(delayMs = 120),
            )
        }
    }
}

// MARK: Schematic map

@Composable
private fun CampusMap(
    venues: List<CampusEventVenue>,
    mapped: List<CampusEventVenue>,
    selectedId: String?,
    onPinTapped: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val line = MaterialTheme.melon.surface.line
    val card = MaterialTheme.melon.surface.card
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainerHigh

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(268.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(surfaceVariant, surfaceContainer)))
            .border(1.dp, MaterialTheme.melon.surface.cardLine, shape),
    ) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight

        // Blueprint grid plus a few soft "building" blocks — decorative only;
        // the pins carry the information.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 40.dp.toPx()
            var x = 0f
            while (x <= size.width) {
                drawLine(line.copy(alpha = line.alpha * 0.5f), Offset(x, 0f), Offset(x, size.height))
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(line.copy(alpha = line.alpha * 0.5f), Offset(0f, y), Offset(size.width, y))
                y += step
            }
        }
        listOf(
            listOf(0.08f, 0.14f, 0.38f, 0.30f),
            listOf(0.56f, 0.10f, 0.34f, 0.40f),
            listOf(0.12f, 0.52f, 0.32f, 0.34f),
            listOf(0.58f, 0.50f, 0.34f, 0.38f),
        ).forEach { (bx, by, bw, bh) ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset((widthPx * bx).roundToInt(), (heightPx * by).roundToInt())
                    }
                    .size(
                        width = (maxWidth * bw),
                        height = (maxHeight * bh),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(card.copy(alpha = 0.55f)),
            )
        }

        mapped.forEach { venue ->
            val isSelected = selectedId == venue.id
            val tone = venueTone(venues, venue)
            val pinSize = if (isSelected) 30.dp else 24.dp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (widthPx * (venue.mapX ?: 0.0) / 100.0).roundToInt(),
                            (heightPx * (venue.mapY ?: 0.0) / 100.0).roundToInt(),
                        )
                    }
                    // Anchor the pin bottom-center on the coordinate, label above.
                    .offset(x = -pinSize / 2, y = if (isSelected) (-58).dp else (-24).dp)
                    .animateContentSize(),
            ) {
                if (isSelected) {
                    Text(
                        text = venue.displayShortName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = MaterialTheme.colorScheme.background,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.onBackground)
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(pinSize)
                        .clip(CircleShape)
                        .background(tone)
                        .border(2.5.dp, card, CircleShape)
                        .clickable { onPinTapped(venue.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = venue.displayShortName,
                        tint = MaterialTheme.melon.fixed.onHero,
                        modifier = Modifier.size(if (isSelected) 16.dp else 13.dp),
                    )
                }
            }
        }
    }
}

// MARK: Venue list

@Composable
private fun VenueList(
    event: CampusEvent,
    venues: List<CampusEventVenue>,
    onTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .campusEventCard(),
    ) {
        venues.forEachIndexed { index, venue ->
            val tone = venueTone(venues, venue)
            val count = event.activityCount(venue)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTap(venue.id) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tone.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = tone,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = venue.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (venue.hint != null) {
                        Text(
                            text = venue.hint.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                }
                if (count > 0) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            if (index < venues.lastIndex) {
                CampusEventRowDivider(startIndent = 67.dp)
            }
        }
    }
}

@Composable
private fun venueTone(venues: List<CampusEventVenue>, venue: CampusEventVenue): Color {
    val palette = campusEventPalette()
    val index = venues.indexOfFirst { it.id == venue.id }.coerceAtLeast(0)
    return palette[index % palette.size]
}
