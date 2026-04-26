package dev.forcetower.unes.designsystem.foundation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.forcetower.unes.designsystem.theme.MelonBrandColors
import dev.forcetower.unes.designsystem.theme.melon
import kotlin.math.cos
import kotlin.math.sin

// Compose port of the JSX/SwiftUI `Mesh` component: animated radial-gradient
// blobs softly orbiting inside a contained rect. The CSS prototype stacks
// blurred opaque circles; we use radial gradients with a 0.85 → 0 alpha falloff
// to get the same "atmospheric" look without depending on the (API-31+) blur
// modifier — that keeps the gradient consistent across our minSdk 28 surface.
//
// Variants match `MeshVariant` on iOS and the JSX `variants` object so the
// same screen reads visually identical on every platform.
enum class MeshVariant { Warm, Cool, Sun, Rose, Fresh }

/**
 * Static blob description. `originX/Y` are fractional positions inside the
 * container (negative values intentionally push off-canvas, matching the CSS
 * `top: -10%; left: -15%` pattern). `period` is the orbit period in seconds;
 * `phaseOffset` desyncs blobs so the field never feels mechanical.
 */
private data class MeshBlob(
    val color: Color,
    val originX: Float,
    val originY: Float,
    val sizeDp: Float,
    val amplitudeX: Float,
    val amplitudeY: Float,
    val period: Float,
    val phaseOffset: Float,
    val ySpeed: Float,
)

@Composable
fun Mesh(
    variant: MeshVariant,
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
) {
    val brand = MaterialTheme.melon.brand
    val blobs = remember(variant, brand) { blobsFor(variant, brand) }

    // Single shared phase, scaled per-blob by `period`. Loops 0→1 over a
    // minute and is multiplied by 60 to recover seconds — long enough that
    // any easing artifacts at the loop boundary go unnoticed.
    val transition = rememberInfiniteTransition(label = "mesh")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mesh-time",
    )

    Canvas(modifier = modifier.clipToBounds()) {
        val w = size.width
        val h = size.height
        val seconds = time * 60f

        blobs.forEach { blob ->
            val phase = (seconds / blob.period) * (2f * Math.PI.toFloat()) + blob.phaseOffset
            val dx = cos(phase) * blob.amplitudeX
            val dy = sin(phase * blob.ySpeed) * blob.amplitudeY
            val scale = 1f + sin(phase * 0.7f) * 0.15f

            val sizePx = blob.sizeDp.dp.toPx()
            val baseLeft = w * blob.originX + dx.dp.toPx()
            val baseTop = h * blob.originY + dy.dp.toPx()
            val cx = baseLeft + sizePx / 2f
            val cy = baseTop + sizePx / 2f
            val radius = (sizePx * scale) / 2f

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to blob.color.copy(alpha = 0.85f * intensity),
                        0.55f to blob.color.copy(alpha = 0.55f * intensity),
                        1f to blob.color.copy(alpha = 0f),
                    ),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }

        // Subtle film-grain highlight, mirroring the CSS overlay. Soft white
        // top-left bias adds warmth without flattening the mesh.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(w * 0.3f, h * 0.2f),
                radius = w * 0.7f,
            ),
            radius = w,
            center = Offset(w * 0.3f, h * 0.2f),
        )
    }
}

@Composable
fun MeshChip(
    variant: MeshVariant,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    cornerRadius: Dp = 14.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius)),
    ) {
        Mesh(variant = variant, modifier = Modifier.fillMaxSize())
    }
}

private fun blobsFor(
    variant: MeshVariant,
    brand: MelonBrandColors,
): List<MeshBlob> = when (variant) {
    MeshVariant.Warm -> listOf(
        MeshBlob(brand.plum, -0.15f, -0.10f, 340f, 40f, 30f, 14f, 0f, 1.1f),
        MeshBlob(brand.coral, 0.50f, 0.30f, 300f, 45f, 25f, 11f, 1.3f, 0.9f),
        MeshBlob(brand.amber, -0.20f, 0.65f, 280f, 35f, 40f, 17f, 2.6f, 1.2f),
        MeshBlob(brand.magenta, 0.55f, 0.60f, 240f, 40f, 30f, 13f, 0.8f, -1.0f),
    )
    MeshVariant.Cool -> listOf(
        MeshBlob(Color(0xFF1E3A5F), -0.10f, -0.10f, 320f, 40f, 30f, 13f, 0f, 1f),
        MeshBlob(Color(0xFF3B9EAE), 0.55f, 0.40f, 280f, 45f, 25f, 15f, 1.5f, 0.8f),
        MeshBlob(Color(0xFF88D4C1), -0.15f, 0.60f, 260f, 35f, 40f, 12f, 2.2f, 1.1f),
    )
    MeshVariant.Sun -> listOf(
        MeshBlob(Color(0xFFC94538), -0.10f, -0.15f, 320f, 40f, 30f, 15f, 0f, 1f),
        MeshBlob(brand.amber, 0.50f, 0.35f, 300f, 45f, 25f, 12f, 1.3f, 0.9f),
        MeshBlob(brand.peach, -0.05f, 0.55f, 280f, 35f, 40f, 14f, 2.4f, 1.15f),
    )
    MeshVariant.Rose -> listOf(
        MeshBlob(Color(0xFF3D1B3E), -0.10f, -0.10f, 300f, 40f, 30f, 14f, 0f, 1f),
        MeshBlob(brand.magenta, 0.50f, 0.30f, 290f, 45f, 25f, 13f, 1.3f, 0.9f),
        MeshBlob(brand.coral, -0.15f, 0.65f, 260f, 35f, 40f, 16f, 2.5f, 1.1f),
    )
    MeshVariant.Fresh -> listOf(
        MeshBlob(Color(0xFF0F4D3A), -0.10f, -0.10f, 320f, 40f, 30f, 13f, 0f, 1f),
        MeshBlob(Color(0xFF4AA679), 0.50f, 0.35f, 290f, 45f, 25f, 14f, 1.3f, 0.9f),
        MeshBlob(brand.amber, -0.10f, 0.65f, 240f, 35f, 40f, 15f, 2.5f, 1.1f),
    )
}
