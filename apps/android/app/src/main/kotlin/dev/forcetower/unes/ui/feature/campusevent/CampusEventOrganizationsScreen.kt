package dev.forcetower.unes.ui.feature.campusevent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventOrganization
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.melon

// The student groups behind (and around) the event. Mirrors iOS
// `CampusEventOrganizationsScreen`: mesh strip on the left, identity + tag +
// details on the right.
@Composable
internal fun CampusEventOrganizationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CampusEventViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val organizations = state.event?.organizations.orEmpty()

    CampusEventDetailScaffold(
        title = stringResource(R.string.campus_event_organizations_title),
        tone = MaterialTheme.melon.palette.magenta,
        onBack = onBack,
        modifier = modifier,
        bottomInset = bottomInset,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.campus_event_organizations_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp, end = 4.dp, bottom = 6.dp)
                    .fadeUpOnAppear(delayMs = 20),
            )

            organizations.forEachIndexed { index, organization ->
                OrganizationCard(
                    organization = organization,
                    index = index,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 40 + index * 30),
                )
            }
        }
    }
}

private val OrganizationMeshes = listOf(
    MeshVariant.Warm, MeshVariant.Rose, MeshVariant.Cool, MeshVariant.Sun, MeshVariant.Fresh,
)

@Composable
private fun OrganizationCard(
    organization: CampusEventOrganization,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val palette = campusEventPalette()
    val tone = palette[index % palette.size]

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .campusEventCard(),
    ) {
        Box(
            modifier = Modifier
                .width(76.dp)
                .fillMaxHeight()
                .background(MaterialTheme.melon.fixed.heroNight),
            contentAlignment = Alignment.Center,
        ) {
            Mesh(
                variant = OrganizationMeshes[index % OrganizationMeshes.size],
                modifier = Modifier.matchParentSize(),
            )
            Icon(
                imageVector = Icons.Outlined.Groups,
                contentDescription = null,
                tint = MaterialTheme.melon.fixed.onHero.copy(alpha = 0.92f),
                modifier = Modifier.size(24.dp),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = organization.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (organization.tag != null) {
                    Text(
                        text = organization.tag.orEmpty(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = tone,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tone.copy(alpha = 0.10f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            if (organization.fullName != null) {
                Text(
                    text = organization.fullName.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            if (organization.details != null) {
                Text(
                    text = organization.details.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
