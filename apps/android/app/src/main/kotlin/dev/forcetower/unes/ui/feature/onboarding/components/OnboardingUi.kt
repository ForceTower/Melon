package dev.forcetower.unes.ui.feature.onboarding.components

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import dev.forcetower.unes.designsystem.theme.LocalMelonDarkTheme
import dev.forcetower.unes.designsystem.theme.MelonMotion

// Shared visual vocabulary for the redesigned onboarding flow
// (dc `UNES Onboarding - Android`): 56-dp pill CTAs built on the native M3
// Button, the pulsing "live" dot, and the status-bar chrome flip for the
// always-dark steps.

/**
 * Drives status/navigation bar icon appearance: light icons while an
 * always-dark step (splash/welcome/sync) is on top, theme-following icons
 * otherwise. Lives at the nav-host level — per-screen save/restore effects
 * race with Nav3's enter-before-dispose ordering and leak the wrong chrome.
 */
@Composable
internal fun SystemBarIconsEffect(darkChrome: Boolean) {
    val view = LocalView.current
    // Resolved theme darkness — respects the Configurações "Tema" override,
    // which `isSystemInDarkTheme()` would ignore.
    val darkTheme = LocalMelonDarkTheme.current
    if (view.isInEditMode) return
    LaunchedEffect(darkChrome, darkTheme) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        val lightIcons = !darkChrome && !darkTheme
        controller.isAppearanceLightStatusBars = lightIcons
        controller.isAppearanceLightNavigationBars = lightIcons
    }
}

/** 56-dp pill CTA — design-spec sizing over the native M3 [Button]. */
@Composable
internal fun OnboardingPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.onBackground,
    contentColor: Color = MaterialTheme.colorScheme.background,
    enabled: Boolean = true,
    loading: Boolean = false,
    showArrow: Boolean = false,
    arrowIcon: ImageVector? = null,
    leadingIcon: ImageVector? = null,
    border: BorderStroke? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = MelonMotion.spring(),
        label = "press-scale",
    )

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = CircleShape,
        border = border,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            // The design dims the whole pill to 45% while keeping its colors.
            disabledContainerColor = containerColor.copy(alpha = containerColor.alpha * 0.45f),
            disabledContentColor = contentColor.copy(alpha = 0.65f),
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 9.dp)
                        .size(22.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.16).sp,
                ),
            )
            if (showArrow && arrowIcon != null) {
                Icon(
                    imageVector = arrowIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 9.dp)
                        .size(20.dp),
                )
            }
        }
    }
}

/** Pulsing "live" dot — the CSS `livePulse` expanding box-shadow ring. */
@Composable
internal fun LivePulseDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
) {
    val transition = rememberInfiniteTransition(label = "live-pulse")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse-t",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            drawCircle(color = color)
            drawCircle(
                color = color.copy(alpha = (1f - t) * 0.55f),
                radius = this.size.minDimension / 2f + t * 5.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
    }
}
