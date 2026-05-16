package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import dev.forcetower.unes.designsystem.theme.melon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale

// Transient overlay between the confirmation sheet and the goodbye view.
// Mirrors `LogoutFlashView` on iOS and `LogoutFlash` in `screens-me.jsx` —
// ~0.9s envelope (fade in fast, hold, fade out slow) wrapping a destructive
// icon tile that pops in with a subtle scale + rotation.
@Composable
internal fun LogoutFlash(modifier: Modifier = Modifier) {
    val envelopeAlpha = remember { Animatable(0f) }
    val iconAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.85f) }
    val iconRotation = remember { Animatable(-6f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                envelopeAlpha.animateTo(1f, tween(durationMillis = 150, easing = EaseOut))
                delay(600)
                envelopeAlpha.animateTo(0f, tween(durationMillis = 150, easing = EaseIn))
            }
            launch {
                // Pop-in: scale 0.85→1.08, rotation -6°→0, alpha 0→1.
                iconAlpha.animateTo(1f, tween(durationMillis = 270, easing = EmphasizedEasing))
            }
            launch {
                iconScale.animateTo(1.08f, tween(durationMillis = 270, easing = EmphasizedEasing))
                iconScale.animateTo(1.0f, tween(durationMillis = 360, easing = EaseOut))
                iconScale.animateTo(0.92f, tween(durationMillis = 270, easing = EaseIn))
            }
            launch {
                iconRotation.animateTo(0f, tween(durationMillis = 270, easing = EmphasizedEasing))
            }
            launch {
                delay(630)
                iconAlpha.animateTo(0.4f, tween(durationMillis = 270, easing = EaseIn))
            }
        }
    }

    val destructive = MaterialTheme.melon.fixed.destructive
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = envelopeAlpha.value }
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(iconScale.value)
                    .rotate(iconRotation.value)
                    .graphicsLayer { alpha = iconAlpha.value }
                    .clip(RoundedCornerShape(14.dp))
                    .background(destructive.copy(alpha = 0.12f))
                    .border(1.dp, destructive.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                MeExitGlyph(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(0.dp))
            Text(
                text = stringResource(R.string.me_logout_flash_caption).uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 10.sp,
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Same easings the Compose Material library exposes by name in iOS — kept
// local so the LogoutFlash file is self-contained.
private val EmphasizedEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val EaseOut = CubicBezierEasing(0f, 0f, 0.2f, 1f)
private val EaseIn = CubicBezierEasing(0.4f, 0f, 1f, 1f)
