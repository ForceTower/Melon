package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.me.ProfileIdentity
import java.util.Locale

// Hero card at the top of the Me screen. The deep `alwaysDarkBg` base anchors
// the whole stack regardless of light/dark mode; the rose mesh sits on top
// (intensity 0.9 to match iOS) and a subtle vertical scrim brings the bottom
// half down so the white text reads cleanly. Rounded 28dp corners + drop
// shadow per JSX prototype.
@Composable
internal fun IdentityCard(identity: ProfileIdentity, modifier: Modifier = Modifier) {
    val brand = MaterialTheme.melon.brand
    // Hero is always-dark — read fixed tokens (no light/dark flip) for the
    // foreground colors so the cream + green stay consistent in both themes.
    val onDark = MaterialTheme.melon.fixed.surfaceLight
    val onDarkSubtle = onDark.copy(alpha = 0.55f)
    val onDarkBody = onDark.copy(alpha = 0.80f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Default shadow tint (black). The 16dp elevation matches the JSX
            // prototype's `0 16px 40px rgba(26,20,32,0.15)` drop-shadow.
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(brand.alwaysDarkBg),
    ) {
        // The card has no explicit height — inside a vertical scroll its
        // vertical constraints are unbounded, so `fillMaxSize` would collapse
        // the mesh and veil to 0dp and we'd see only the flat dark base.
        // `matchParentSize` defers measurement until the content Column
        // reports its height (same fix NowCard uses).
        Mesh(
            variant = MeshVariant.Rose,
            intensity = 0.9f,
            modifier = Modifier.matchParentSize(),
        )
        // Vertical scrim — top transparent, bottom dimmer — matches the JSX
        // `linear-gradient(180deg, rgba(26,15,40,0.15) → rgba(26,15,40,0.6))`
        // overlay that keeps the avatar/text readable over the warm mesh.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to brand.alwaysDarkBg.copy(alpha = 0.15f),
                        1f to brand.alwaysDarkBg.copy(alpha = 0.6f),
                    ),
                ),
        )

        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
            EyebrowRow(identity = identity, onDark = onDark, onDarkSubtle = onDarkSubtle)
            Spacer(Modifier.height(18.dp))
            AvatarRow(
                identity = identity,
                onDark = onDark,
                onDarkBody = onDarkBody,
                onDarkSubtle = onDarkSubtle,
            )
            Spacer(Modifier.height(18.dp))
            StatsRail(identity = identity, onDark = onDark, onDarkSubtle = onDarkSubtle)
        }
    }
}

@Composable
private fun EyebrowRow(identity: ProfileIdentity, onDark: Color, onDarkSubtle: Color) {
    val username = identity.username.ifBlank { "—" }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.me_identity_card_username_format, username)
                .uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = onDark.copy(alpha = 0.65f),
            modifier = Modifier.weight(1f),
        )
        StudentCardPill(onDark = onDark, onDarkSubtle = onDarkSubtle)
    }
}

@Composable
private fun StudentCardPill(onDark: Color, @Suppress("UNUSED_PARAMETER") onDarkSubtle: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(onDark.copy(alpha = 0.12f))
            .border(1.dp, onDark.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MeQrGlyph(color = onDark, modifier = Modifier.size(11.dp))
        Text(
            text = stringResource(R.string.me_identity_card_pill).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                letterSpacing = 0.95.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = onDark,
        )
    }
}

@Composable
private fun AvatarRow(
    identity: ProfileIdentity,
    onDark: Color,
    onDarkBody: Color,
    onDarkSubtle: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Avatar(initial = identity.avatarInitial)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = identity.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    lineHeight = 25.sp,
                    letterSpacing = (-0.36).sp,
                ),
                color = onDark,
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = identity.course,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = onDarkBody,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = identity.campus,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.36.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = onDarkSubtle,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Avatar(initial: String) {
    val brand = MaterialTheme.melon.brand
    val ok = MaterialTheme.melon.fixed.ok
    val brandPlum = brand.alwaysDarkBg
    // Outer wrapper isn't clipped — the avatar circle clips on its own inner
    // Box so the active-indicator dot can sit at the bottom-right corner with
    // its border bleeding past the circle's edge (matches iOS, where the dot
    // is offset to (+22,+22) outside the avatar's frame).
    Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = brand.coral.copy(alpha = 0.4f),
                    spotColor = brand.coral.copy(alpha = 0.4f),
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        0f to brand.amber,
                        0.55f to brand.coral,
                        1f to brand.magenta,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    lineHeight = 32.sp,
                    letterSpacing = (-0.64).sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = MaterialTheme.melon.fixed.surfaceLight,
            )
        }
        // Active indicator dot — sibling of the clipped circle so the
        // 2.5dp plum border can render on top of (and slightly outside) the
        // avatar's gradient fill.
        //
        // Positioned via center + offset(22, 22) to match iOS exactly: a 14dp
        // dot whose center sits on the avatar's bottom-right perimeter (the
        // circle's edge at 45° is at +22.6dp from center). `BottomEnd`
        // alignment would shove the dot into the corner of the 64dp frame
        // and leave it floating outside the circle entirely.
        Box(
            modifier = Modifier
                .offset(x = 22.dp, y = 22.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(ok)
                .border(2.5.dp, brandPlum, CircleShape),
        )
    }
}

@Composable
private fun StatsRail(identity: ProfileIdentity, onDark: Color, onDarkSubtle: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(onDark.copy(alpha = 0.15f)),
        )
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            IdentityStat(
                label = stringResource(R.string.me_identity_card_stat_cr),
                // NaN means the overallScore flow hasn't emitted yet — render
                // an em-dash rather than the misleading 0,0 a numeric format
                // would produce.
                value = if (identity.cr.isNaN()) {
                    "—"
                } else {
                    String.format(Locale.forLanguageTag("pt-BR"), "%.1f", identity.cr)
                },
                accent = identity.crDelta.takeIf { it.isNotEmpty() },
                onDark = onDark,
                onDarkSubtle = onDarkSubtle,
                modifier = Modifier.weight(1f),
            )
            IdentityStat(
                label = stringResource(R.string.me_identity_card_stat_credits),
                value = identity.creditsDone.toString(),
                sub = stringResource(R.string.me_identity_card_stat_total_format, identity.creditsRequired),
                onDark = onDark,
                onDarkSubtle = onDarkSubtle,
                modifier = Modifier.weight(1f),
            )
            IdentityStat(
                label = stringResource(R.string.me_identity_card_stat_semester),
                value = identity.semesterWeek.toString(),
                sub = stringResource(R.string.me_identity_card_stat_weeks_format, identity.semesterTotalWeeks),
                onDark = onDark,
                onDarkSubtle = onDarkSubtle,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun IdentityStat(
    label: String,
    value: String,
    onDark: Color,
    onDarkSubtle: Color,
    modifier: Modifier = Modifier,
    sub: String? = null,
    accent: String? = null,
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.26.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = onDarkSubtle,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.48).sp,
                ),
                color = onDark,
            )
            if (sub != null) {
                Spacer(Modifier.size(3.dp))
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = onDarkSubtle,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
        if (!accent.isNullOrEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = "↑ $accent",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.melon.fixed.okOnDark,
            )
        }
    }
}
