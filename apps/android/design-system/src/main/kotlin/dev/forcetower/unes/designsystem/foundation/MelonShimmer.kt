package dev.forcetower.unes.designsystem.foundation

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon

// Skeleton shimmer — a plate fill with a highlight band that sweeps across it
// while content loads.
//
// The band lives in *root* coordinates and is clocked off the shared frame
// time, so every shimmering element on screen shows the same single sheen
// passing left-to-right at once, no matter when each one was composed. That
// is what makes a list of skeleton rows read as one loading surface instead
// of a field of independently blinking capsules.
//
// Previews render the resting plate with no motion.

/** Capsule ends — the default for text-line placeholders. */
val SkeletonShape: Shape = RoundedCornerShape(percent = 50)

// Sheen band width as a fraction of the root width; the sweep travels one
// band past each edge so it enters and exits fully.
private const val SheenWidthFraction = 0.55f
private const val SheenTilt = 0.35f

fun Modifier.shimmer(shape: Shape = SkeletonShape): Modifier = composed {
    val plate = MaterialTheme.melon.surface.skeletonPlate
    val sheen = MaterialTheme.melon.surface.skeletonSheen
    val animated = !LocalInspectionMode.current

    // Read only inside `drawBehind` so each frame invalidates draw, not
    // composition.
    var progress by remember { mutableFloatStateOf(0f) }
    var rootX by remember { mutableFloatStateOf(0f) }
    var rootWidth by remember { mutableFloatStateOf(0f) }

    if (animated) {
        LaunchedEffect(Unit) {
            val period = MelonMotion.ShimmerPeriodMillis
            while (true) {
                withInfiniteAnimationFrameMillis { frame ->
                    progress = (frame % period) / period.toFloat()
                }
            }
        }
    }

    this
        .onGloballyPositioned { coordinates ->
            rootX = coordinates.positionInRoot().x
            rootWidth = coordinates.findRootCoordinates().size.width.toFloat()
        }
        .clip(shape)
        .drawBehind {
            drawRect(plate)
            if (!animated || rootWidth <= 0f) return@drawBehind
            val band = rootWidth * SheenWidthFraction
            val travel = rootWidth + 2 * band
            val startX = -band + travel * progress - rootX
            drawRect(
                brush = Brush.linearGradient(
                    0f to Color.Transparent,
                    0.5f to sheen,
                    1f to Color.Transparent,
                    start = Offset(startX, 0f),
                    end = Offset(startX + band, band * SheenTilt),
                ),
            )
        }
}

/** A fixed-size shimmering capsule standing in for a line of text or a thumbnail. */
@Composable
fun SkeletonBar(width: Dp, height: Dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width, height).shimmer())
}

@Preview
@Composable
private fun SkeletonPreview() {
    MelonTheme {
        Surface {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(20.dp),
            ) {
                SkeletonBar(width = 56.dp, height = 80.dp)
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SkeletonBar(width = 70.dp, height = 9.dp)
                    SkeletonBar(width = 240.dp, height = 13.dp)
                    SkeletonBar(width = 150.dp, height = 10.dp)
                }
            }
        }
    }
}
