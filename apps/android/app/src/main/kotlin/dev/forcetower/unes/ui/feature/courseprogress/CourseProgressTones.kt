package dev.forcetower.unes.ui.feature.courseprogress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumEntryStatus
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon

// How a slot's outline is drawn. The design's rule is that a situation is
// never carried by color alone: each one pairs a distinct glyph with a
// distinct edge treatment, so the grid stays readable in greyscale and for
// color-blind readers.
internal enum class CurriculumStatusEdge { Solid, Thick, Dashed, Hatch, Faint }

@Immutable
internal data class CurriculumStatusStyle(
    val tone: Color,
    // Fraction of `tone` washed behind the slot; 0 leaves it on the card.
    val fillAlpha: Float,
    val edge: CurriculumStatusEdge,
    val icon: ImageVector,
    val label: String,
    // Abbreviated for chips and grid cells.
    val shortLabel: String,
    // "Cumprida" is the only one that reads as a finished, filled mark.
    val filled: Boolean,
)

// Situation → theme tone. Every color routes through an existing token:
// approval/failure ride the semantic status trio, "cursando" and "apto" take
// palette hues already used elsewhere in the app, and the two inert
// situations fall back to the neutral outlines so they recede.
@Composable
@ReadOnlyComposable
internal fun curriculumStatusTone(status: CurriculumEntryStatus): Color = when (status) {
    CurriculumEntryStatus.Completed -> MaterialTheme.melon.status.ok
    CurriculumEntryStatus.InProgress -> MaterialTheme.melon.palette.sky
    CurriculumEntryStatus.Available -> MaterialTheme.melon.palette.violet
    CurriculumEntryStatus.Withdrawn -> MaterialTheme.melon.palette.orange
    CurriculumEntryStatus.Failed -> MaterialTheme.melon.status.bad
    CurriculumEntryStatus.Blocked -> MaterialTheme.colorScheme.outline
    CurriculumEntryStatus.NotTaken -> MaterialTheme.colorScheme.outlineVariant
}

@Composable
internal fun curriculumStatusStyle(status: CurriculumEntryStatus): CurriculumStatusStyle =
    CurriculumStatusStyle(
        tone = curriculumStatusTone(status),
        fillAlpha = when (status) {
            CurriculumEntryStatus.Completed -> 0.12f
            CurriculumEntryStatus.InProgress, CurriculumEntryStatus.Failed -> 0.10f
            else -> 0f
        },
        edge = when (status) {
            CurriculumEntryStatus.InProgress -> CurriculumStatusEdge.Thick
            CurriculumEntryStatus.Withdrawn -> CurriculumStatusEdge.Hatch
            CurriculumEntryStatus.Blocked -> CurriculumStatusEdge.Dashed
            CurriculumEntryStatus.NotTaken -> CurriculumStatusEdge.Faint
            else -> CurriculumStatusEdge.Solid
        },
        icon = when (status) {
            CurriculumEntryStatus.Completed -> Icons.Filled.Check
            CurriculumEntryStatus.InProgress -> Icons.Filled.RadioButtonChecked
            CurriculumEntryStatus.Available -> Icons.Filled.Add
            CurriculumEntryStatus.Withdrawn -> Icons.Filled.Pause
            CurriculumEntryStatus.Failed -> Icons.Filled.Close
            CurriculumEntryStatus.Blocked -> Icons.Filled.Lock
            CurriculumEntryStatus.NotTaken -> Icons.Filled.RadioButtonUnchecked
        },
        label = stringResource(curriculumStatusLabelRes(status)),
        shortLabel = stringResource(curriculumStatusShortLabelRes(status)),
        filled = status == CurriculumEntryStatus.Completed,
    )

private fun curriculumStatusLabelRes(status: CurriculumEntryStatus): Int = when (status) {
    CurriculumEntryStatus.Completed -> R.string.course_progress_status_completed
    CurriculumEntryStatus.InProgress -> R.string.course_progress_status_in_progress
    CurriculumEntryStatus.Available -> R.string.course_progress_status_available
    CurriculumEntryStatus.Withdrawn -> R.string.course_progress_status_withdrawn
    CurriculumEntryStatus.Failed -> R.string.course_progress_status_failed
    CurriculumEntryStatus.Blocked -> R.string.course_progress_status_blocked
    CurriculumEntryStatus.NotTaken -> R.string.course_progress_status_not_taken
}

private fun curriculumStatusShortLabelRes(status: CurriculumEntryStatus): Int = when (status) {
    CurriculumEntryStatus.InProgress -> R.string.course_progress_status_in_progress_short
    CurriculumEntryStatus.Available -> R.string.course_progress_status_available_short
    CurriculumEntryStatus.Blocked -> R.string.course_progress_status_blocked_short
    else -> curriculumStatusLabelRes(status)
}

// Paints a slot's plate: the tonal wash plus the edge treatment its situation
// owns. `Modifier.border` would cover the plain outlines, but dashed and
// hatched need their own passes — and those two are the whole point of the
// treatment, reading as "interrupted" and "paused" at a glance. Callers clip
// to the same corner so the hatch stays inside the slot.
internal fun Modifier.curriculumSlotSurface(
    style: CurriculumStatusStyle,
    corner: Dp,
    faintEdge: Color,
): Modifier = drawBehind {
    val radius = corner.toPx()
    if (style.fillAlpha > 0f) {
        drawRoundRect(
            color = style.tone.copy(alpha = style.fillAlpha),
            cornerRadius = CornerRadius(radius),
        )
    }
    if (style.edge == CurriculumStatusEdge.Hatch) drawHatch(style.tone.copy(alpha = 0.16f))

    val strokeWidth = if (style.edge == CurriculumStatusEdge.Thick) 2.dp.toPx() else 1.dp.toPx()
    val inset = strokeWidth / 2f
    drawRoundRect(
        color = when (style.edge) {
            CurriculumStatusEdge.Faint -> faintEdge
            CurriculumStatusEdge.Thick -> style.tone
            CurriculumStatusEdge.Dashed, CurriculumStatusEdge.Hatch -> style.tone.copy(alpha = 0.75f)
            CurriculumStatusEdge.Solid -> style.tone.copy(alpha = 0.55f)
        },
        topLeft = Offset(inset, inset),
        size = Size(size.width - strokeWidth, size.height - strokeWidth),
        cornerRadius = CornerRadius((radius - inset).coerceAtLeast(0f)),
        style = Stroke(
            width = strokeWidth,
            pathEffect = if (style.edge == CurriculumStatusEdge.Dashed) DashedEdge else null,
        ),
    )
}

private val DashedEdge = PathEffect.dashPathEffect(floatArrayOf(9f, 7f))

// 45° stripes — the "trancada" treatment.
private fun DrawScope.drawHatch(color: Color) {
    val step = 9.dp.toPx()
    var x = -size.height
    while (x < size.width) {
        drawLine(
            color = color,
            start = Offset(x, size.height),
            end = Offset(x + size.height, 0f),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Butt,
        )
        x += step
    }
}

// The square glyph that stands for a slot everywhere: períodos rows, the map,
// the grid cells and the detail sheet. Always icon + shape, never color alone.
@Composable
internal fun CurriculumStatusBadge(
    status: CurriculumEntryStatus,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    corner: Dp = 10.dp,
    iconSize: Dp = 17.dp,
) {
    val style = curriculumStatusStyle(status)
    val faintEdge = MaterialTheme.melon.surface.line
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .then(
                if (style.filled) {
                    Modifier.drawBehind { drawRect(style.tone) }
                } else {
                    Modifier.curriculumSlotSurface(style, corner, faintEdge)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = if (style.filled) MaterialTheme.melon.fixed.onHero else style.tone,
            modifier = Modifier.size(iconSize),
        )
    }
}

// Fades everything outside an active trail without removing it — the chain is
// only legible against the rest of the grid.
internal fun Modifier.trailDim(dimmed: Boolean): Modifier =
    if (dimmed) alpha(TrailDimAlpha) else this

private const val TrailDimAlpha = 0.22f
