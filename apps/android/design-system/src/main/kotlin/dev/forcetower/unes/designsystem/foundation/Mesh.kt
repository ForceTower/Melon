package dev.forcetower.unes.designsystem.foundation

import android.os.Build
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.forcetower.unes.designsystem.theme.MelonBrandColors
import dev.forcetower.unes.designsystem.theme.melon
import kotlin.math.cos
import kotlin.math.sin

// Compose port of the SwiftUI `MeshGradientView`: solid blobs softly orbiting
// inside a contained rect, smeared by a 48dp Gaussian to give the field its
// atmospheric, "no visible edges" feel. iOS draws solid 0.85-alpha discs then
// applies `.blur(radius: 48)` per blob — on API 31+ we mirror that exactly
// with `Modifier.blur`. On API 28-30 the blur modifier is a no-op, so we
// fall back to a Gaussian-shaped multi-stop radial alpha that approximates
// the same falloff without the visible shoulder of the old 3-stop curve.
//
// Variants match `MeshVariant` on iOS and the JSX `variants` object so the
// same screen reads visually identical on every platform.
enum class MeshVariant { Warm, Cool, Sun, Rose, Fresh }

private val BlurRadius = 48.dp
private val SupportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

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

    // Monotonic wall-clock seconds, mirroring iOS's `TimelineView(.animation)`
    // + `Date().timeIntervalSince1970`. A finite tween won't work here because
    // the blob periods (11, 13, 14, 16, 17) aren't commensurate, so any
    // restart would snap the phases visibly when the loop wrapped.
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withInfiniteAnimationFrameNanos { it }
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                seconds = (now - start) / 1_000_000_000f
            }
        }
    }

    val baseAlpha = (0.85f * intensity).coerceIn(0f, 1f)

    // Mesh redraws every frame via `withInfiniteAnimationFrameNanos` but doesn't
    // go through Compose's animation system, so it never votes a frame rate.
    // On API 35+ that leaves `AndroidComposeView.currentFrameRate` at its
    // post-draw reset of `NaN`, and Samsung verbosely logs every
    // `setRequestedFrameRate(NaN)` call. Anchor a 30 Hz vote here — blob orbits
    // are 11–17s with sub-50dp amplitudes, so 30 fps is visually identical to
    // 60 and lets the panel downclock for the ambient field.
    Box(modifier = modifier.preferredFrameRate(30f).clipToBounds()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (SupportsBlur) Modifier.blur(BlurRadius, BlurredEdgeTreatment.Unbounded)
                    else Modifier,
                ),
        ) {
            val w = size.width
            val h = size.height
            val t = seconds

            blobs.forEach { blob ->
                val phase = (t / blob.period) * (2f * Math.PI.toFloat()) + blob.phaseOffset
                val dx = cos(phase) * blob.amplitudeX
                val dy = sin(phase * blob.ySpeed) * blob.amplitudeY
                val scale = 1f + sin(phase * 0.7f) * 0.15f

                val sizePx = blob.sizeDp.dp.toPx()
                val baseLeft = w * blob.originX + dx.dp.toPx()
                val baseTop = h * blob.originY + dy.dp.toPx()
                val cx = baseLeft + sizePx / 2f
                val cy = baseTop + sizePx / 2f
                val radius = (sizePx * scale) / 2f

                if (SupportsBlur) {
                    drawCircle(
                        color = blob.color.copy(alpha = baseAlpha),
                        radius = radius,
                        center = Offset(cx, cy),
                    )
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to blob.color.copy(alpha = baseAlpha),
                                0.35f to blob.color.copy(alpha = (0.78f * intensity).coerceIn(0f, 1f)),
                                0.6f to blob.color.copy(alpha = (0.45f * intensity).coerceIn(0f, 1f)),
                                0.82f to blob.color.copy(alpha = (0.16f * intensity).coerceIn(0f, 1f)),
                                1f to blob.color.copy(alpha = 0f),
                            ),
                            center = Offset(cx, cy),
                            radius = radius,
                        ),
                        radius = radius,
                        center = Offset(cx, cy),
                    )
                }
            }
        }

        // Film-grain highlight stays on its own un-blurred layer so it reads
        // crisp on top of the diffused blobs, matching iOS where the highlight
        // is filled on the parent context without the per-blob blur filter.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
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
