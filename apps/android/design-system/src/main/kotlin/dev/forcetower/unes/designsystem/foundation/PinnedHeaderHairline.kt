package dev.forcetower.unes.designsystem.foundation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon

// Bottom edge of a pinned screen header: a hairline that fades in once the
// content behind it starts scrolling, so rows visibly slide under fixed
// chrome — the Compose analogue of the iOS large-title divider.
@Composable
fun PinnedHeaderHairline(scrolled: Boolean, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = MelonMotion.ease(),
        label = "pinned-header-hairline",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(MaterialTheme.melon.surface.line),
    )
}
