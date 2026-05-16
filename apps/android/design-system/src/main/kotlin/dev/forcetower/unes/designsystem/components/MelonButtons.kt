package dev.forcetower.unes.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon

// Mirrors iOS `PrimaryButton` / `GhostButton` / `GlassButton` from
// `apps/ios/UNES/DesignSystem/UNESButtons.swift`. 54-dp tall pill, press-scale
// 0.97, optional trailing arrow + loading spinner.
//
// Accessibility: every clickable carries `Role.Button` so TalkBack announces
// it as a button (not a generic clickable element). The label stays in
// semantics across loading state — the spinner replaces the *visual* but not
// the *announcement*. Decorative artwork (arrow glyph, leading icons) is
// excluded from the a11y tree via `clearAndSetSemantics`.

private val PillShape = RoundedCornerShape(percent = 50)

@Composable
fun MelonPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showsArrow: Boolean = true,
    isLoading: Boolean = false,
    background: Color = MaterialTheme.colorScheme.onBackground,
    contentColor: Color = MaterialTheme.colorScheme.surface,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressBg = if (pressed) MaterialTheme.colorScheme.onSurfaceVariant else background

    val loadingDescription = stringLoadingState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(if (pressed) 0.97f else 1f)
            .clip(PillShape)
            .background(pressBg)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = text,
                onClick = onClick,
            )
            .then(
                if (isLoading) {
                    Modifier.semantics { stateDescription = loadingDescription }
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        // The label is always laid out so screen readers can read it; we hide
        // it visually behind the spinner while loading rather than removing
        // the Text composable, which would erase the semantics.
        ProvideTextStyle(
            MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.17).sp,
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    color = contentColor,
                    modifier = Modifier.alpha(if (isLoading) 0f else 1f),
                )
                if (isLoading) {
                    CircularSpinner(
                        color = contentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (showsArrow && !isLoading) {
                ArrowRightGlyph(color = contentColor)
            }
        }
    }
}

@Composable
fun MelonGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val ink = MaterialTheme.colorScheme.onBackground
    val line = MaterialTheme.melon.surface.line

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(if (pressed) 0.97f else 1f)
            .clip(PillShape)
            .background(
                if (pressed) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            )
            .border(1.5.dp, line, PillShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = text,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        if (leading != null) {
            Box(modifier = Modifier.clearAndSetSemantics {}) { leading() }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.17).sp,
            ),
            color = ink,
        )
    }
}

/**
 * "Liquid glass" pill — translucent fill + subtle stroke, designed to sit on
 * top of the warm mesh on splash/welcome. iOS 26 gets a real glass effect; on
 * earlier OSes (and on Android) we approximate with a low-alpha tint.
 */
@Composable
fun MelonGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    foreground: Color = Color(0xFFFBF7F2),
    tint: Color = Color.White.copy(alpha = 0.08f),
    stroke: Color = Color.White.copy(alpha = 0.14f),
    leading: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(if (pressed) 0.97f else 1f)
            .clip(PillShape)
            .background(tint)
            .border(1.dp, stroke, PillShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = text,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        if (leading != null) {
            Box(modifier = Modifier.clearAndSetSemantics {}) { leading() }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.17).sp,
            ),
            color = foreground,
        )
    }
}

@Composable
internal fun ArrowRightGlyph(
    color: Color = LocalContentColor.current,
    modifier: Modifier = Modifier.size(18.dp),
) {
    // Pure decoration — the parent button's `onClickLabel` carries the
    // semantic meaning. Strip semantics so screen readers don't read it
    // as a separate node.
    Canvas(modifier.clearAndSetSemantics {}) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8f * density, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.5f)
            lineTo(w * 0.78f, h * 0.5f)
            moveTo(w * 0.78f, h * 0.5f)
            lineTo(w * 0.53f, h * 0.25f)
            moveTo(w * 0.78f, h * 0.5f)
            lineTo(w * 0.53f, h * 0.75f)
        }
        drawPath(path, color = color, style = stroke)
    }
}

/** 75% trim, rotating spinner. Mirrors iOS `SpinnerView`. */
@Composable
internal fun CircularSpinner(
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    strokeWidth: Dp = 2.dp,
) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin",
    )

    // Indeterminate progress — TalkBack announces "in progress" instead of
    // ignoring this as an unlabeled spinning canvas.
    Canvas(
        modifier
            .rotate(rotation)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
    ) {
        val sw = strokeWidth.toPx()
        val inset = sw / 2f
        val arcSize = Size(size.width - sw, size.height - sw)
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun stringLoadingState(): String = "Carregando"
