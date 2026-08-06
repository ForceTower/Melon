package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.RevealShadow
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.formatGrade
import dev.forcetower.unes.ui.feature.me.MeFixtures
import dev.forcetower.unes.ui.feature.me.ProfileIdentity
import kotlin.math.abs

// Identity mesh card — dc `EuScreen` hero. Always-dark `heroNight` plate with
// the warm brand mesh drifting behind a legibility scrim; avatar with the
// edit badge, name/course, the "UEFS · Módulo" chip, and the
// Score · Frequência · Semestre stat row below a hairline divider. Tapping
// the identity row opens the profile-customization sheet.
@Composable
internal fun IdentityCard(
    identity: ProfileIdentity,
    modifier: Modifier = Modifier,
    revealDelayMs: Int = 120,
    onEditProfile: (() -> Unit)? = null,
) {
    val fixed = MaterialTheme.melon.fixed
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Reveal and shadow travel together: handing the elevation to the
            // reveal keeps it out of the fading layer, which would clip it to
            // the card's own bounds for the length of the animation.
            .scaleInOnAppear(
                delayMs = revealDelayMs,
                fromScale = 0.97f,
                shadow = RevealShadow(elevation = 12.dp, shape = shape),
            )
            .clip(shape)
            .background(fixed.heroNight),
    ) {
        // `matchParentSize` (not `fillMaxSize`) — inside a scroll column the
        // card's height comes from the content Column; the mesh and scrim
        // defer to it instead of collapsing to 0dp.
        Mesh(variant = MeshVariant.Hero, modifier = Modifier.matchParentSize())
        // Legibility scrim: dim top and bottom, breathe in the middle —
        // matches the dc `linear-gradient(180deg, .28 / .10 45% / .34)` veil.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to fixed.heroVeil.copy(alpha = 0.28f),
                        0.45f to fixed.heroVeil.copy(alpha = 0.10f),
                        1f to fixed.heroVeil.copy(alpha = 0.34f),
                    ),
                ),
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
            IdentityRow(identity, onEditProfile = onEditProfile)
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(fixed.onHero.copy(alpha = 0.16f)),
            )
            Spacer(Modifier.height(16.dp))
            StatsRow(identity)
        }
    }
}

@Composable
private fun IdentityRow(identity: ProfileIdentity, onEditProfile: (() -> Unit)?) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = if (onEditProfile == null) {
            Modifier
        } else {
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.me_edit_profile_action),
                    onClick = onEditProfile,
                )
        },
    ) {
        Avatar(
            initial = identity.avatarInitial,
            avatarUrl = identity.avatarUrl,
            showEditBadge = onEditProfile != null,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = identity.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 23.sp,
                    letterSpacing = (-0.4).sp,
                ),
                color = onHero,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = identity.course,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = onHero.copy(alpha = 0.86f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(9.dp))
            CampusChip(
                label = identity.campusLabel
                    ?: stringResource(R.string.me_identity_campus_fallback),
            )
        }
    }
}

@Composable
private fun CampusChip(label: String) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(onHero.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.School,
            contentDescription = null,
            tint = onHero.copy(alpha = 0.85f),
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label.uppercase(LocalConfiguration.current.locales[0]),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.88.sp,
            ),
            color = onHero,
            maxLines = 1,
        )
    }
}

@Composable
private fun Avatar(initial: String, avatarUrl: String?, showEditBadge: Boolean) {
    val brand = MaterialTheme.melon.brand
    val fixed = MaterialTheme.melon.fixed
    Box(modifier = Modifier.size(66.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = fixed.heroVeil,
                    spotColor = fixed.heroVeil,
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        0f to brand.amber,
                        0.52f to brand.coral,
                        1f to brand.magenta,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            // The gradient initial stays underneath as the loading/error
            // fallback — the photo simply paints over it once it arrives.
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = fixed.onHero,
            )
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        // Edit badge on the avatar's bottom-right perimeter — dc renders a
        // pencil in a heroNight pastille with a faint onHero ring.
        if (showEditBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(fixed.onHero.copy(alpha = 0.22f))
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(fixed.heroNight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = fixed.onHero.copy(alpha = 0.9f),
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun StatsRow(identity: ProfileIdentity) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Stat(
            label = stringResource(R.string.me_stat_score),
            value = formatGrade(identity.cr),
            modifier = Modifier.weight(1f),
        ) {
            ScoreDelta(delta = identity.crDelta)
        }
        StatDivider()
        Stat(
            label = stringResource(R.string.me_stat_attendance),
            value = identity.attendancePercent?.toString() ?: "–",
            valueSuffix = identity.attendancePercent?.let { "%" },
            modifier = Modifier
                .weight(1f)
                .padding(start = 18.dp),
        ) {
            StatCaption(text = stringResource(R.string.me_stat_attendance_caption))
        }
        StatDivider()
        Stat(
            label = stringResource(R.string.me_stat_semester),
            value = identity.semesterOrdinal
                ?.let { stringResource(R.string.me_stat_semester_ordinal_format, it) }
                ?: "–",
            modifier = Modifier
                .weight(1f)
                .padding(start = 18.dp),
        ) {
            StatCaption(text = stringResource(R.string.me_stat_semester_caption))
        }
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(MaterialTheme.melon.fixed.onHero.copy(alpha = 0.14f)),
    )
}

@Composable
private fun Stat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueSuffix: String? = null,
    footer: @Composable () -> Unit,
) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(LocalConfiguration.current.locales[0]),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
            ),
            color = onHero.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 26.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.52).sp,
                ),
                color = onHero,
            )
            if (valueSuffix != null) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = valueSuffix,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = onHero.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 1.dp),
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        footer()
    }
}

@Composable
private fun StatCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
        color = MaterialTheme.melon.fixed.onHero.copy(alpha = 0.6f),
    )
}

// Rising/falling CR arrow — rendered only when the movement is visible after
// truncation (|delta| ≥ 0.1), the same rule iOS applies.
@Composable
private fun ScoreDelta(delta: Double?) {
    if (delta == null || abs(delta) < 0.1) {
        StatCaption(text = stringResource(R.string.me_stat_score_caption))
        return
    }
    val rising = delta >= 0
    val color = if (rising) {
        MaterialTheme.melon.fixed.live
    } else {
        MaterialTheme.melon.status.bad
    }
    val icon: ImageVector = if (rising) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = formatGrade(abs(delta)),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                letterSpacing = 0.sp,
            ),
            color = color,
        )
    }
}

@Preview
@Composable
private fun IdentityCardPreview() {
    MelonTheme {
        IdentityCard(identity = MeFixtures.identity, modifier = Modifier.padding(20.dp))
    }
}

@Preview
@Composable
private fun IdentityCardDarkPreview() {
    MelonTheme(darkTheme = true) {
        IdentityCard(identity = MeFixtures.identity, modifier = Modifier.padding(20.dp))
    }
}
