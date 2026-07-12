package dev.forcetower.unes.ui.feature.campusevent

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventSpeaker
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon

// The guest list. Rows expand in place to show the bio, so the only state is
// which card is open — kept in the composable. Mirrors iOS
// `CampusEventSpeakersScreen`.
@Composable
internal fun CampusEventSpeakersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CampusEventViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val speakers = state.event?.speakers.orEmpty()
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    CampusEventDetailScaffold(
        title = stringResource(R.string.campus_event_speakers_title),
        tone = MaterialTheme.melon.palette.violet,
        onBack = onBack,
        modifier = modifier,
        bottomInset = bottomInset,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.campus_event_speakers_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
                    .fadeUpOnAppear(delayMs = 20),
            )

            speakers.forEachIndexed { index, speaker ->
                SpeakerCard(
                    speaker = speaker,
                    isExpanded = expandedId == speaker.id,
                    onTap = {
                        expandedId = if (expandedId == speaker.id) null else speaker.id
                    },
                    modifier = Modifier.fadeUpOnAppear(delayMs = 40 + index * 30),
                )
            }
        }
    }
}

@Composable
private fun SpeakerCard(
    speaker: CampusEventSpeaker,
    isExpanded: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val violet = MaterialTheme.melon.palette.violet
    val hasBio = speaker.bio != null
    val chevron by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = MelonMotion.ease(),
        label = "chevron",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .campusEventCard()
            .clickable(enabled = hasBio, onClick = onTap)
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            CampusEventAvatar(name = speaker.name, size = 46.dp)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        text = speaker.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.5.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (speaker.tag != null) {
                        Text(
                            text = speaker.tag.orEmpty(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.sp,
                            ),
                            color = violet,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(violet.copy(alpha = 0.10f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                val subtitle = listOfNotNull(speaker.role, speaker.organization)
                    .joinToString(" · ")
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (hasBio) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(chevron),
                )
            }
        }

        if (isExpanded && speaker.bio != null) {
            CampusEventRowDivider(startIndent = 0.dp)
            Text(
                text = speaker.bio.orEmpty(),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 15.dp, end = 15.dp, top = 12.dp, bottom = 15.dp),
            )
        }
    }
}
