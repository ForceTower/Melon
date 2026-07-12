package dev.forcetower.unes.ui.feature.campusevent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventAudience
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventCategory
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventWorkshop
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.melon

// Hands-on workshops, grouped by audience (freshmen → veterans → shared).
// Mirrors iOS `CampusEventWorkshopsScreen`.
@Composable
internal fun CampusEventWorkshopsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CampusEventViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val workshops = state.event?.workshops.orEmpty()

    val groups = listOf(
        CampusEventAudience.Freshmen,
        CampusEventAudience.Veterans,
        CampusEventAudience.Everyone,
    ).mapNotNull { audience ->
        val matching = workshops.filter { it.audience == audience }
        if (matching.isEmpty()) null else audience to matching
    }

    CampusEventDetailScaffold(
        title = stringResource(R.string.campus_event_workshops_title),
        tone = MaterialTheme.melon.palette.teal,
        onBack = onBack,
        modifier = modifier,
        bottomInset = bottomInset,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.campus_event_workshops_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp, end = 4.dp, bottom = 18.dp)
                    .fadeUpOnAppear(delayMs = 20),
            )

            groups.forEachIndexed { index, (audience, groupWorkshops) ->
                Column(
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .fadeUpOnAppear(delayMs = 60 + index * 60),
                ) {
                    if (groups.size > 1) {
                        // The header carries its own 12dp bottom padding, so
                        // it sits outside the spaced card stack.
                        CampusEventSectionHeader(
                            title = stringResource(
                                when (audience) {
                                    CampusEventAudience.Freshmen -> R.string.campus_event_workshops_freshmen
                                    CampusEventAudience.Veterans -> R.string.campus_event_workshops_veterans
                                    CampusEventAudience.Everyone -> R.string.campus_event_workshops_everyone
                                },
                            ),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        groupWorkshops.forEach { workshop ->
                            WorkshopCard(workshop)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkshopCard(workshop: CampusEventWorkshop, modifier: Modifier = Modifier) {
    val tone = workshop.audience.tone()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .campusEventCard()
            .padding(15.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tone.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = CampusEventCategory.Workshop.icon,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(19.dp),
                )
            }
            Text(
                text = workshop.title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
        }

        if (workshop.details != null) {
            Text(
                text = workshop.details.orEmpty(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 14.dp),
        ) {
            if (workshop.instructors != null) {
                MetaChip(
                    icon = Icons.Outlined.Person,
                    text = workshop.instructors.orEmpty(),
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            if (workshop.venueName != null) {
                MetaChip(
                    icon = Icons.Outlined.LocationOn,
                    text = workshop.venueName.orEmpty(),
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        CampusEventRowDivider(startIndent = 0.dp)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SignupBadge(workshop)
            Spacer(Modifier.weight(1f))
            CampusEventAudienceChip(audience = workshop.audience)
        }
    }
}

@Composable
private fun MetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SignupBadge(workshop: CampusEventWorkshop) {
    if (workshop.requiresSignup) {
        val warn = MaterialTheme.melon.status.warn
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(MaterialTheme.melon.palette.amber.copy(alpha = 0.12f))
                .padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ConfirmationNumber,
                contentDescription = null,
                tint = warn,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = if (workshop.slots != null) {
                    pluralStringResource(
                        R.plurals.campus_event_workshops_signup_slots,
                        workshop.slots ?: 0,
                        workshop.slots ?: 0,
                    )
                } else {
                    stringResource(R.string.campus_event_workshops_signup)
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    letterSpacing = 0.sp,
                ),
                color = warn,
            )
        }
    } else {
        val ok = MaterialTheme.melon.status.ok
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(ok.copy(alpha = 0.12f))
                .padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = ok,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = stringResource(R.string.campus_event_workshops_all_in),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    letterSpacing = 0.sp,
                ),
                color = ok,
            )
        }
    }
}
