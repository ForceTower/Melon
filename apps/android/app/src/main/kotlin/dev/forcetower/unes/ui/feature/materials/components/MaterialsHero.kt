package dev.forcetower.unes.ui.feature.materials.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.theme.melon

// The "feito por estudantes" mesh plate (dc hub hero + list empty state):
// always-dark card with coral/amber/magenta blobs drifting under a plum veil.
// Content sits on top; callers compose the copy + CTAs they need.
@Composable
internal fun MaterialsHeroPlate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val melon = MaterialTheme.melon
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(melon.fixed.heroNight),
    ) {
        Mesh(
            colors = listOf(melon.brand.coral, melon.brand.amber, melon.brand.magenta),
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to melon.fixed.heroVeil.copy(alpha = 0.34f),
                        1f to melon.fixed.heroVeil.copy(alpha = 0.68f),
                    ),
                ),
        )
        content()
    }
}

// "✦ FEITO POR ESTUDANTES" pill on the hero.
@Composable
internal fun MaterialsHeroBadge(modifier: Modifier = Modifier) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(onHero.copy(alpha = 0.16f))
            .padding(horizontal = 11.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = onHero,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = stringResource(R.string.materials_hero_badge).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.55.sp,
            ),
            color = onHero,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
